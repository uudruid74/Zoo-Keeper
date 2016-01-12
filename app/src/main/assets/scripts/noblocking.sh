#!/system/bin/sh

BASE="/system/etc"
PATH="/system/xbin:$PATH"

mount -o remount,rw /system

# Backup Original
if [ ! -f "$BASE/hosts.orig" ]; then
	cp "$BASE/hosts" "$BASE/hosts.orig"
fi

# If it's a link, unlink it
if [ -h "$BASE/hosts" ]; then
	rm -f "$BASE/hosts"
fi

cat <<END >$BASE/hosts
127.0.0.1 localhost
127.0.0.1 localhost.localdomain
127.0.0.1 android
127.0.0.1 android.localhost
127.0.0.1 test
127.0.0.1 test.localhost
127.0.0.1 debian
127.0.0.1 debian.localhost
END

mount -o remount,ro /system
