# controller/ — two very different controllers

## CallController.java — recruiter-facing
- All responses go through CallSessionResponse DTO
- CallSessionResponse must NEVER include phone numbers (real or virtual)
- Returns: session ID, status, duration, recruiter ID, lead ID, timestamps

## TwilioWebhookController.java — Twilio-facing
- Returns TwiML (XML), not JSON
- Must validate X-Twilio-Signature on every request
- Is the ONLY place where decryptForTwilio() is called
- Any change here needs a manual Twilio sandbox test — no unit test can cover it fully
- Content-Type of responses must be application/xml or Twilio ignores them