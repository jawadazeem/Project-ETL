import os
from google.cloud import storage as gcp_storage
from google.auth.credentials import AnonymousCredentials
from google.cloud.exceptions import Conflict
from azure.storage.blob import BlobServiceClient
from azure.core.exceptions import ResourceExistsError
from botocore.exceptions import ClientError
import boto3
import io
from concurrent.futures import ThreadPoolExecutor

AZURITE_ACCOUNT_NAME = "devstoreaccount1"
AZURITE_ACCOUNT_KEY = (
    "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/"
    "K1SZFPTOtr/KBHBeksoGMGw=="
)
DEFAULT_AZURITE_BLOB_ENDPOINT = "http://127.0.0.1:10000/devstoreaccount1"
DEFAULT_AZURITE_API_VERSION = "2023-11-03"


class MultiCloudStorageManager:
    def __init__(self, environment="local"):
        self.environment = environment
        self.s3_client = None
        self.azure_client = None
        self.gcp_client = None
        self._init_aws()
        self._init_gcp()
        self._init_azure()

    def _init_aws(self):
        """Initializes AWS S3 Client targeting LocalStack if local."""
        if self.environment == "local":
            self.s3_client = boto3.client(
                "s3",
                endpoint_url="http://localhost:4568",
                aws_access_key_id="mock_key",
                aws_secret_access_key="mock_secret",
                region_name="us-east-1"
            )
        else:
            self.s3_client = boto3.client("s3")

    def _init_gcp(self):
        """Initializes GCP Storage Client targeting fake-gcs-server if local."""
        if self.environment == "local":
            # fake-gcs-server routes via STORAGE_EMULATOR_HOST environment variable
            os.environ.setdefault("STORAGE_EMULATOR_HOST", "http://127.0.0.1:4443")
            self.gcp_client = gcp_storage.Client(
                credentials=AnonymousCredentials(),
                project="mock-project"
            )
        else:
            self.gcp_client = gcp_storage.Client()

    def _init_azure(self):
        """Initializes Azure Blob Service Client targeting Azurite if local."""
        if self.environment == "local":
            azurite_conn_str = os.getenv("AZURE_STORAGE_CONNECTION_STRING")
            if not azurite_conn_str:
                blob_endpoint = os.getenv("AZURITE_BLOB_ENDPOINT", DEFAULT_AZURITE_BLOB_ENDPOINT)
                azurite_conn_str = (
                    "DefaultEndpointsProtocol=http;"
                    f"AccountName={AZURITE_ACCOUNT_NAME};"
                    f"AccountKey={AZURITE_ACCOUNT_KEY};"
                    f"BlobEndpoint={blob_endpoint};"
                )
            self.azure_client = BlobServiceClient.from_connection_string(
                azurite_conn_str,
                api_version=os.getenv("AZURITE_API_VERSION", DEFAULT_AZURITE_API_VERSION),
            )
        else:
            self.azure_client = BlobServiceClient.from_connection_string(
                os.getenv("AZURE_STORAGE_CONNECTION_STRING")
            )

    # AWS S3 Operations
    def create_bucket_aws(self, bucket_name):
        try:
            self.s3_client.create_bucket(Bucket=bucket_name)
            print(f"AWS Bucket '{bucket_name}' created successfully.")
        except ClientError:
            print(f"AWS Bucket '{bucket_name}' already exists.")


    def upload_to_aws(self, local_path, bucket_name, object_name):
        """Uploads a local file to AWS S3 bucket."""
        self.s3_client.upload_file(local_path, bucket_name, object_name)
        print(f"Uploaded {local_path} to AWS S3 bucket '{bucket_name}' as '{object_name}'")

    def download_from_aws(self, bucket_name, object_name, local_dest_path):
        """Downloads an object from AWS S3 to the filesystem."""
        self.s3_client.download_file(bucket_name, object_name, local_dest_path)
        print(f"Downloaded '{object_name}' from AWS S3 to {local_dest_path}")

    # GCP Cloud Storage Operations
    def create_bucket_gcp(self, bucket_name):
        try:
            self.gcp_client.create_bucket(bucket_name)
            print(f"GCP Bucket '{bucket_name}' created successfully.")
        except Conflict:
            print(f"GCP Bucket '{bucket_name}' already exists.")

    def upload_to_gcp(self, local_path, bucket_name, blob_name):
        """Uploads a local file to GCP Cloud Storage bucket."""
        bucket = self.gcp_client.bucket(bucket_name)
        blob = bucket.blob(blob_name)
        blob.upload_from_filename(local_path)
        print(f"Uploaded {local_path} to GCP GCS bucket '{bucket_name}' as '{blob_name}'")

    def download_from_gcp(self, bucket_name, blob_name, local_dest_path):
        """Downloads a blob from GCP Cloud Storage to the filesystem."""
        bucket = self.gcp_client.bucket(bucket_name)
        blob = bucket.blob(blob_name)
        blob.download_to_filename(local_dest_path)
        print(f"Downloaded '{blob_name}' from GCP GCS to {local_dest_path}")

    def create_bucket_azure(self, bucket_name):
        try:
            # Create container with public blob access enabled
            self.azure_client.create_container(bucket_name, public_access="blob")
            print(f"Azure Container '{bucket_name}' created successfully with public blob access.")
        except ResourceExistsError:
            print(f"Azure Container '{bucket_name}' already exists.")
            # Pass an empty dict {} for signed_identifiers to satisfy the positional argument
            container_client = self.azure_client.get_container_client(bucket_name)
            container_client.set_container_access_policy({}, public_access="blob")

    def upload_to_azure(self, local_path, container_name, blob_name):
        """Uploads a local file to an Azure Blob Storage container."""
        blob_client = self.azure_client.get_blob_client(container=container_name, blob=blob_name)
        with open(local_path, "rb") as data:
            blob_client.upload_blob(data, overwrite=True)
        print(f"Uploaded {local_path} to Azure Container '{container_name}' as '{blob_name}'")

    def download_from_azure(self, container_name, blob_name, local_dest_path):
        """Downloads a blob from Azure Blob Storage to the filesystem."""
        blob_client = self.azure_client.get_blob_client(container=container_name, blob=blob_name)
        with open(local_dest_path, "wb") as file:
            download_stream = blob_client.download_blob()
            file.write(download_stream.readall())
        print(f"Downloaded '{blob_name}' from Azure to {local_dest_path}")

    # Easy interface for managing all of the buckets
    def create_buckets(self, bucket_name: str):
        self.create_bucket_aws(bucket_name)
        self.create_bucket_azure(bucket_name)
        self.create_bucket_gcp(bucket_name)

    def upload(self,
               aws_local_path: str,
               gcp_local_path: str,
               azure_local_path: str,
               bucket_name: str,
               period: str):
        
        self.upload_to_aws(aws_local_path, bucket_name, period)
        self.upload_to_gcp(gcp_local_path, bucket_name, period)
        self.upload_to_azure(azure_local_path, bucket_name, period)

    def upload_csv_bytes(self, csv_data: bytes, bucket_name: str, object_name: str):
        """Uploads raw CSV bytes to AWS, GCP, and Azure concurrently without writing to disk."""
        
        def _aws_upload():
            # boto3 put_object accepts raw bytes/file-like buffers directly
            self.s3_client.put_object(
                Bucket=bucket_name,
                Key=object_name,
                Body=csv_data
            )

        def _gcp_upload():
            bucket = self.gcp_client.bucket(bucket_name)
            blob = bucket.blob(object_name)
            blob.upload_from_string(csv_data, content_type="text/csv")

        def _azure_upload():
            blob_client = self.azure_client.get_blob_client(container=bucket_name, blob=object_name)
            blob_client.upload_blob(csv_data, overwrite=True)

        # Execute all three cloud writes concurrently to avoid blocking
        with ThreadPoolExecutor(max_workers=3) as executor:
            f1 = executor.submit(_aws_upload)
            f2 = executor.submit(_gcp_upload)
            f3 = executor.submit(_azure_upload)
            # Wait for all uploads to complete
            f1.result()
            f2.result()
            f3.result()

        print(f"Uploaded updated CSV in-memory to all clouds for '{bucket_name}/{object_name}'")