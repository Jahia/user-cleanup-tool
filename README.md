# User Cleanup Tool

A Jahia administration tool that finds and removes `jnt:ace` and `jnt:member`
nodes that reference users or groups no longer known to the system — for
example, ACE entries or group memberships left behind after a user was removed
from an LDAP directory.

## How it works

The module contributes an admin tool page at **Administration → Tools →
`cleanup-users`** (protected by Jahia's tool access token). The page:

- lists the configured **external user/group providers** and warns if any are
  currently inactive (so you do not delete references that only look orphaned
  because their provider is stopped);
- lists `jnt:ace` nodes whose `j:principal` user/group no longer exists;
- lists `jnt:member` nodes whose referenced member node no longer exists;
- lets you select entries and delete them (paged, 25 per page).

Deletion runs in a system session and is restricted to `jnt:ace` / `jnt:member`
nodes as a safety guard.

## Caution

Before cleaning, make sure **all** of your external providers appear in the
provider list. If a provider is stopped, its users look unknown and their
references would be wrongly flagged as orphaned. The tool surfaces a warning
when an inactive provider is detected.

## Build

```sh
mvn clean install
```
