# Track Specification: Fix Duplicate Keys and Auto-Delete

## Issues
1. java.lang.IllegalArgumentException: Key "source-..." was already used in SourcesScreen.
2. Episodes are not deleting automatically once marked as watched.

## Success Criteria
- No crashes in SourcesScreen due to duplicate keys.
- Episodes delete automatically according to preferences (Remove after marked as read) in internal player, external player, and manual marking.
