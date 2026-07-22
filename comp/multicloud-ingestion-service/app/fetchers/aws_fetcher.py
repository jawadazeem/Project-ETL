import boto3

class AWSBillingFetcher:
    def __init__(self, role_arn: str, external_id: str):
        # 1. Assume customer's role
        sts_client = boto3.client('sts')
        assumed_role = sts_client.assume_role(
            RoleArn=role_arn,
            RoleSessionName="MultiCloudIngestionSession",
            ExternalId=external_id
        )
        
        creds = assumed_role['Credentials']
        
        # 2. Authenticate S3 client using short-lived tokens
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=creds['AccessKeyId'],
            aws_secret_access_key=creds['SecretAccessKey'],
            aws_session_token=creds['SessionToken']
        )

    def fetch_latest_report(self, bucket_name: str, object_key: str) -> bytes:
        response = self.s3_client.get_object(Bucket=bucket_name, Key=object_key)
        return response['Body'].read()