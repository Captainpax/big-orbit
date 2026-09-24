# Security policy

Please do not file a public issue for a vulnerability that could expose an owner credential,
device key binding, account metadata, or Little Orbit infrastructure. Contact the repository
owner privately through the security-reporting channel configured on GitHub.

Include the affected version, a minimal reproduction, and impact. Do not include real tokens,
passwords, bootstrap PINs or tokens, TOTP secrets, recovery codes, relationship content, or
production database data.

Big Orbit never accepts arbitrary administration commands. A report that demonstrates a way
to bypass PIN attempt/expiry/consumption, use a bootstrap bearer on an ordinary admin route,
skip device binding, replay a challenge, replace existing MFA during enrollment, cross
administrator accounts, or obtain private relationship data is high priority.

The published stable release is 1.3.0 code 3. Its independently signed APK is compatible
with the published Little Orbit 1.3.0 API; deferred physical-device observations are recorded
separately and are not presented as completed security evidence.
