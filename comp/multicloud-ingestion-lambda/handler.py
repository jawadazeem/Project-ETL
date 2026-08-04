import csv
import io
import json
import logging
import os
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import urlopen

import boto3

logger = logging.getLogger()
logger.setLevel(logging.INFO)

BILLING_BUCKET = os.getenv("BILLING_BUCKET", "cloud-billing")
AWS_ENDPOINT_URL = os.getenv("AWS_ENDPOINT_URL")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")
AWS_ACCESS_KEY_ID = os.getenv("AWS_ACCESS_KEY_ID", "test")
AWS_SECRET_ACCESS_KEY = os.getenv("AWS_SECRET_ACCESS_KEY", "test")

DEMO_SOURCE_BUCKET = os.getenv("DEMO_SOURCE_BUCKET", "billingreports")
DEMO_SOURCE_OBJECT_KEY = os.getenv("DEMO_SOURCE_OBJECT_KEY", "2026-06.csv")
DEMO_AWS_ENDPOINT_URL = os.getenv("DEMO_AWS_ENDPOINT_URL", "http://azeem-corp-aws-storage:4566")
DEMO_AZURE_BLOB_ENDPOINT = os.getenv(
    "DEMO_AZURE_BLOB_ENDPOINT",
    "http://azeem-corp-azure-storage:10000/devstoreaccount1",
)
DEMO_GCP_STORAGE_ENDPOINT = os.getenv(
    "DEMO_GCP_STORAGE_ENDPOINT",
    "http://azeem-corp-gcp-storage:4443",
)

BLUEPRINT_HEADER = [
    "Account_Name",
    "Resource_ID",
    "Cloud_Provider",
    "Billing_Period",
    "Compute_Hours",
    "Storage_GB_Used",
    "API_Requests",
    "Total_Charge",
    "Service_Name",
    "Description",
]


def handler(event, context):
    requests = _poll_requests_from_event(event)
    if not requests:
        logger.info("No cloud connection poll requests found in EventBridge event.")
        return {"processed": 0}

    s3_client = _s3_client(AWS_ENDPOINT_URL)
    written = []
    failures = []

    for target_prefix, prefix_requests in _group_requests_by_target(requests).items():
        rows = []

        for request in prefix_requests:
            try:
                raw_data = _fetch_raw_report(request)
                rows.extend(_normalize_provider_report(request, raw_data))
            except Exception as exc:
                failure = {
                    "id": request.get("id"),
                    "provider": request.get("provider"),
                    "error": str(exc),
                }
                failures.append(failure)
                logger.exception("Failed to process cloud connection poll request: %s", failure)

        if not rows:
            logger.warning("No billing rows were produced for target prefix %s", target_prefix)
            continue

        key = _target_key(target_prefix)
        body = _billing_csv(rows)
        s3_client.put_object(
            Bucket=BILLING_BUCKET,
            Key=key,
            Body=body,
            ContentType="text/csv",
        )
        written.append(f"s3://{BILLING_BUCKET}/{key}")
        logger.info(
            "Wrote %s normalized billing rows to s3://%s/%s",
            len(rows),
            BILLING_BUCKET,
            key,
        )

    return {
        "processed": len(requests),
        "objects": written,
        "failures": failures,
    }


def _poll_requests_from_event(event):
    detail = event.get("detail", event)
    if isinstance(detail, str):
        detail = json.loads(detail)
    if isinstance(detail, list):
        return detail
    return detail.get("requests", [])


def _group_requests_by_target(requests):
    grouped = {}
    for request in requests:
        prefix = _target_prefix(request)
        grouped.setdefault(prefix, []).append(request)
    return grouped


def _target_prefix(request):
    prefix = request["ownerBlueprintBucketKey"].strip("/")
    if prefix.startswith(f"{BILLING_BUCKET}/"):
        prefix = prefix[len(BILLING_BUCKET) + 1 :]
    return prefix


def _target_key(prefix):
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    return f"{prefix}/cloud-connection-sync-{timestamp}.csv"


def _fetch_raw_report(request):
    provider = _provider(request)
    bucket = request.get("clientBucketName") or DEMO_SOURCE_BUCKET
    object_key = _source_object_key(request)

    try:
        return _fetch_provider_report(provider, bucket, object_key)
    except Exception:
        if bucket != DEMO_SOURCE_BUCKET:
            logger.warning(
                "Could not fetch %s/%s for %s; retrying demo bucket %s",
                bucket,
                object_key,
                provider,
                DEMO_SOURCE_BUCKET,
            )
            return _fetch_provider_report(provider, DEMO_SOURCE_BUCKET, object_key)
        raise


def _fetch_provider_report(provider, bucket, object_key):
    if provider == "AWS":
        return _fetch_aws_report(bucket, object_key)
    if provider == "AZURE":
        return _fetch_azure_report(bucket, object_key)
    if provider == "GCP":
        return _fetch_gcp_report(bucket, object_key)

    raise ValueError(f"Unsupported cloud provider: {provider}")


def _source_object_key(request):
    credentials = request.get("credentials") or {}
    return (
        request.get("sourceObjectKey")
        or credentials.get("sourceObjectKey")
        or credentials.get("objectKey")
        or credentials.get("blobName")
        or DEMO_SOURCE_OBJECT_KEY
    )


def _fetch_aws_report(bucket, object_key):
    response = _s3_client(DEMO_AWS_ENDPOINT_URL).get_object(Bucket=bucket, Key=object_key)
    return response["Body"].read()


def _fetch_azure_report(container, blob_name):
    endpoint = DEMO_AZURE_BLOB_ENDPOINT.rstrip("/")
    url = f"{endpoint}/{quote(container)}/{quote(blob_name)}"
    return _http_get(url)


def _fetch_gcp_report(bucket, object_key):
    endpoint = DEMO_GCP_STORAGE_ENDPOINT.rstrip("/")
    encoded_object = quote(object_key, safe="")
    json_api_url = f"{endpoint}/storage/v1/b/{quote(bucket)}/o/{encoded_object}?alt=media"

    try:
        return _http_get(json_api_url)
    except RuntimeError:
        public_url = f"{endpoint}/{quote(bucket)}/{quote(object_key)}"
        return _http_get(public_url)


def _http_get(url):
    try:
        with urlopen(url, timeout=10) as response:
            return response.read()
    except HTTPError as exc:
        raise RuntimeError(f"HTTP {exc.code} while fetching {url}") from exc
    except URLError as exc:
        raise RuntimeError(f"Could not fetch {url}: {exc.reason}") from exc


def _normalize_provider_report(request, raw_data):
    provider = _provider(request)
    text = raw_data.decode("utf-8-sig")
    reader = csv.DictReader(io.StringIO(text))

    if provider == "AWS":
        return [_aws_row(row) for row in reader]
    if provider == "AZURE":
        return [_azure_row(row) for row in reader]
    if provider == "GCP":
        return [_gcp_row(row) for row in reader]

    raise ValueError(f"Unsupported cloud provider: {provider}")


def _aws_row(row):
    return [
        row.get("lineItem/UsageAccountId", ""),
        row.get("lineItem/ResourceId", ""),
        "AWS",
        row.get("bill/BillingPeriodStartDate", ""),
        _number(row.get("lineItem/UsageAmount_ComputeHours")),
        _number(row.get("lineItem/UsageAmount_StorageGB")),
        _integer(row.get("lineItem/UsageAmount_APIRequests")),
        _number(row.get("lineItem/UnblendedCost")),
        row.get("product/ProductName", ""),
        row.get("lineItem/LineItemDescription", ""),
    ]


def _azure_row(row):
    return [
        row.get("SubscriptionName", ""),
        row.get("ResourceId", ""),
        "AZURE",
        row.get("BillingMonth", ""),
        _number(row.get("Quantity_Compute")),
        _number(row.get("Quantity_Storage")),
        _integer(row.get("Quantity_API")),
        _number(row.get("CostInBillingCurrency")),
        row.get("ConsumedService", ""),
        row.get("MeterName", ""),
    ]


def _gcp_row(row):
    return [
        row.get("project.id", ""),
        row.get("resource.name", ""),
        "GCP",
        row.get("usage_start_time", ""),
        _number(row.get("usage.amount_compute")),
        _number(row.get("usage.amount_storage")),
        _integer(row.get("usage.amount_api")),
        _number(row.get("cost")),
        row.get("service.description", ""),
        row.get("sku.description", ""),
    ]


def _billing_csv(rows):
    buffer = io.StringIO()
    writer = csv.writer(buffer)
    writer.writerow(BLUEPRINT_HEADER)
    writer.writerows(rows)
    return buffer.getvalue().encode("utf-8")


def _s3_client(endpoint_url):
    return boto3.client(
        "s3",
        endpoint_url=endpoint_url,
        aws_access_key_id=AWS_ACCESS_KEY_ID,
        aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
        region_name=AWS_REGION,
    )


def _provider(request):
    return (request.get("provider") or "").upper()


def _number(value):
    if value in (None, ""):
        return "0.0"
    return str(float(value))


def _integer(value):
    if value in (None, ""):
        return "0"
    return str(int(float(value)))
