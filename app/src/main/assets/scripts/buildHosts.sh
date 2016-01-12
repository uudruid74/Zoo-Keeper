#!/system/bin/sh

BASE="/system/etc"
PATH="/system/xbin:$PATH"
ABASE="/storage/emulated/0/Android"
mount -o remount,rw /system

# Backup Original
if [ ! -f "$BASE/hosts.orig" ]; then
        cp "$BASE/hosts" "$BASE/hosts.orig"
fi

# If it's a link, unlink it
if [ -h "$BASE/hosts" ]; then
        rm -f "$BASE/hosts"
fi

# Copy, avoiding whitelisted
if [ -f "$ABASE/hosts.whitelist" ]; then
        WHITEREGEX=`sed ':begin;$!N;s/\n/ -e /;tbegin' "$ABASE/hosts.whitelist"`
        # build hosts, ignoring whitelist
        grep -v -e $WHITEREGEX "$BASE/hosts.orig" >"$BASE/hosts"
else
        cp "$BASE/hosts.orig" "$BASE/hosts"
fi

# Blacklist extra hosts
if [ -f "$ABASE/hosts.blacklist" ]; then
        sed 's/^/127.0.0.1\t/g' "$ABASE/hosts.blacklist" >>"$BASE/hosts"
fi

mount -o remount,ro /system