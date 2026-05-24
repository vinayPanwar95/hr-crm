# hr-calling-service — Twilio calling with number masking

## What this service owns
Outbound call initiation, Twilio webhook handling, call session storage,
call recordings. Number masking — recruiters never see real candidate numbers.

## What this service does NOT own
- Real phone numbers live in hr-lead-service — fetch via Feign client
- Call transcription and AI summaries — handled by hr-ai-service
- Sending post-call WhatsApp follow-ups — handled by hr-whatsapp-service

## The most critical rule in this service
NEVER log, return, or expose realNumberEncrypted or decrypted phone numbers
in any API response, log line, or error message. This is a GDPR/legal requirement.
If you see code that does this, flag it before proceeding.

## Security rules (read before editing anything)
- realNumberEncrypted column: only MaskingService may read or write it
- decryptForTwilio() method: only called from TwilioWebhookController
- No recruiter-facing endpoint may return phone numbers in any form
- Twilio webhook endpoints must validate X-Twilio-Signature header

## Package layout
com.hrcrm.calling
├── controller/    CallController (recruiter-facing), TwilioWebhookController (Twilio-facing)
├── service/       CallService, MaskingService, CallRecordingService
├── repository/    CallSessionRepository
├── model/entity/  CallSession
├── model/dto/     InitiateCallRequest, CallSessionResponse (no phone numbers in response!)
├── client/        LeadServiceClient (Feign)
└── config/        TwilioConfig, EncryptionConfig

## When you add a new endpoint
Ask yourself: could this endpoint expose a phone number, even indirectly?
If yes, stop and consult the security rules above.

## Running locally
Twilio webhooks need a public URL. Use ngrok:
ngrok http 8083
Update TWILIO_WEBHOOK_BASE_URL in .env to the ngrok URL