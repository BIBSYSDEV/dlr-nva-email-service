# DLR NVA email service

## Usage
This API is only available to other services internal at the same aws account.

## Requirements
AWS requires access to SES by either applying for production access, 
or by verifying to_email addresses manually in the console.

Parameter "ApiDomain" must be a route 53 DNS in the same account, in order to set up verified domain automatically.


## Sending requests
to_address, text_html, text and subject are required fields.

Optional from_address can be set, but it has to be verified first manually with AWS SES.

### Sample
POST:
```json
    {
  "from_address": "noreply@test.com",
  "to_address": "test1@test.com",
  "cc": "test2@test.com",
  "bcc": "test3@test.com",
  "subject": "test subject",
  "text": "Raw text",
  "text_html": "<h1>Some html text here</h1>"
}
```
