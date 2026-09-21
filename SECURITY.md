# Security policy

Please do not file a public issue for a vulnerability that could expose an owner credential,
device key binding, account metadata, or Little Orbit infrastructure. Contact the repository
owner privately through the security-reporting channel configured on GitHub.

Include the affected version, a minimal reproduction, and impact. Do not include real tokens,
passwords, TOTP secrets, recovery codes, relationship content, or production database data.

Big Orbit never accepts arbitrary administration commands. A report that demonstrates a way
to bypass device binding, replay a challenge, cross administrator accounts, or obtain private
relationship data is high priority.
