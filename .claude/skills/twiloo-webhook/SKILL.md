---
name: twilio-webhook
description: >
  Use this skill when adding a new Twilio webhook handler or modifying how
  the calling service responds to Twilio callbacks. Triggers: "add webhook",
  "handle Twilio callback", "new TwiML response", "Twilio status update",
  "call completed hook", "recording callback", "add a new Twilio event".
---

## What a Twilio webhook is
Twilio calls YOUR endpoint when something happens (call answered, call ended,
recording ready). You respond with TwiML XML telling Twilio what to do next.

## Checklist for every new webhook handler

1. Add @PostMapping to TwilioWebhookController (never a new controller)
2. Validate the signature first — before any business logic:
```java
   twilioSignatureValidator.validate(request); // throws if invalid
```
3. Return ResponseEntity<String> with ContentType APPLICATION_XML
4. Wrap business logic in try-catch — Twilio retries on non-200 responses,
   which causes duplicate processing. Catch exceptions, log, return 200 anyway.
5. Never do heavy work synchronously — publish an event, return 200 immediately
6. Add the webhook URL to TwilioConfig so it is centrally managed

## TwiML response template
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <!-- your TwiML verbs here -->
</Response>
```

## Common TwiML verbs
- <Dial> — connect to another number
- <Record> — start recording
- <Say> — text to speech
- <Pause> — wait N seconds
- <Hangup> — end call

## What NOT to do
- Never return phone numbers in TwiML responses beyond the immediate dial target
- Never throw exceptions from webhook handlers — always return 200
- Never call Twilio APIs synchronously inside a webhook — risk of timeout loops