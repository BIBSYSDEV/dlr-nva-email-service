# DLR NVA email service

## Usage
This API is only available to other services internal at the same aws account.

## Requirements
AWS requires access to SES by either applying for production access, 
or by verifying to_email addresses manually in the console.

## Sending requests
from_name, to_name and subject are required fields.
Atleast one of text and text_html must be present.

### Sample
POST:
```json
    {
  "from_name": "noreply@test.com",
  "to_name": "test1@test.com",
  "cc": "test2@test.com",
  "bcc": "test3@test.com",
  "subject": "test subject",
  "text": "Raw text",
  "text_html": "<h1>Some html text here</h1>"
}
```
