#!/system/bin/sh
#
# All scripts and other info is stored on the Internal Storage in Cron
# Busybox sees these in Unix locations via the following symlinks
# The "root" crontab is the "master" file, created dynamically and
# then the user's "advanced" additions (if any) are appended.
#
USERDIR="/data/media/0/ZooKeeper/Cron"
SUBDIRS="hourly daily weekly monthly"
mount -o remount,rw /system
echo "root:x:0:0::/system/etc/crontabs:/system/bin/sh" > /system/etc/passwd
mkdir /system/etc/crontabs
chown root.root /system/etc/crontabs
chmod 775 /system/etc/crontabs
mkdir -p $USERDIR
chown -R media_rw:media_rw /data/media/0/ZooKeeper
chmod 777 $USERDIR
for DIR in $SUBDIRS; do
    mkdir /system/etc/cron.$DIR
    chown root.root /system/etc/cron.$DIR
    chmod 775 /system/etc/cron.$DIR
    if [ "$DIR" == "hourly" ]; then
        cat >$USERDIR/$DIR <<EOH
#!/system/bin/sh
#
# This is a simple shell script, just add your own commands!
#
date
ZooToast "Running $DIR crontab"
echo

EOH
    else
        cat >$USERDIR/$DIR <<EOL
#!/system/bin/sh
#
# This is a simple shell script, just add your own commands!
#
date
ZooNotify "Running $DIR crontab" "file:///sdcard/ZooKeeper/Cron/cronlog.txt"
echo

EOL
    fi
    chown root.root $USERDIR/$DIR
    chmod 777 $USERDIR/$DIR
    ln -s $USERDIR/$DIR /system/etc/cron.$DIR/zookeeper
done

cat >$USERDIR/advanced <<"EOADV"
# This file is in cron format.   The time and date fields are:
#
#              field          allowed values
#              -----          --------------
#              minute         0-59
#              hour           0-23
#              day of month   1-31
#              month          0-12 (or names, see below)
#              day of week    0-7 (0 or 7 is Sun, or use names)
#
# Examples:
#       # run at 10 pm on weekdays, annoy Joe
#       0 22 * * 1-5   mail -s "It's 10pm" joe%Joe,%%Where are your kids?%
#       # check for updates saturday morning at 2:10am
#       10 2 * * 6     am startservice -n "systems.eddon.android.zoo_keeper/.NotifyDownloader" --es Action Upgrade
#       # Reminder
#       0 23 * * 6     ZooToast Dont forget you have church in the morning
#

EOADV

CRONFILES="simple advanced systemupdate"
for file in $CRONFILES; do
    echo >$USERDIR/$file
    chmod 666 $USERDIR/$file
    chown media_rw.media_rw $USERDIR/$file
done

echo >$USERDIR/master
chmod 666 $USERDIR/master
chown root.root $USERDIR/master

cat >/system/xbin/ZooToast <<"EOTCH"
#!/system/bin/sh
# This is a little script to display Toasts from other scripts
am start -n "systems.eddon.android.zoo_keeper/.Notify" --es Message "$*"
echo "$*"

EOTCH

cat >/system/xbin/ZooNotify <<"EONTF"
#!/system/bin/sh
# This is a little script to display Notifications from other scripts
# It takes 3 arguments (quote them). The message, the URL to go to on a click, and a type
# If no type is specified, it defaults to "text/plain"
#
MESSAGE="$1"
URL="$2"
TYPE="$3"

if [ -z "$URL" ]; then
        URL="file://$0"
fi

if [ -z "$3" ]; then
    am startservice -n "systems.eddon.android.zoo_keeper/.Notify" --es Message "$MESSAGE" --es Title $0 --es PID $$ --es URL "$URL"
else
    am startservice -n "systems.eddon.android.zoo_keeper/.Notify" --es Message "$MESSAGE" --es Title $0 --es PID $$ --es URL "$URL" --es Type "$TYPE"
fi
echo "$MESSAGE"

EONTF

chmod 775 /system/xbin/ZooToast
chmod 775 /system/xbin/ZooNotify

# The magic link!
ln -s $USERDIR/master /system/etc/crontabs/root

cat >/system/etc/init.d/10restoreData <<"EORESTORE"
#!/system/xbin/bash
#
# Stupidly Simple Auto-Restore start-up script - EKL
#

TAG="/data/media/0/ZooKeeper/restore-on-boot"
if [ -f "$TAG" ]; then
    echo "Restoring Backup Data!"
    rm $TAG
    /data/media/0/ZooKeeper/restore.sh
fi

EORESTORE
chmod 755 /system/etc/init.d/10restoreData

cat >/system/etc/init.d/20crond <<"EOCD"
#!/system/xbin/bash
#
# Stupidly Simple cron start-up script - EKL
#

action=$1

#
# Sane PATH
#
PATH="/bin:/system/bin:$PATH"
export PATH

#
# crond has "/bin/sh" hardcoded
#
if [ ! -d "/bin" ]; then
    mount -o remount,rw rootfs /
    ln -s /system/xbin /bin
    mount -o remount,ro rootfs /
fi

#
# A few options
#
TZ=`cat /system/etc/timezone`
export TZ

CRONSTOP="/storage/emulated/0/ZooKeeper/Cron/.cronstop"
USERDIR="/data/media/0/ZooKeeper/Cron"
CRONDIR="/system/etc/crontabs"
# OPTS=`cat /system/etc/cronopts`
OPTS="-L /storage/emulated/0/ZooKeeper/Cron/cronlog.txt"

#
# Now the actual service start
#
case $action in
stop)
    echo "Killing cron daemon ..."
    touch $CRONSTOP
    killall crond
    ;;
restart)
    echo "Restarting Cron ..."
    killall crond
    ;&
start)
    if [ -f "$CRONSTOP" ]; then
        rm $CRONSTOP
    fi
    ;&
*)
    chmod 777 $USERDIR $USERDIR/hourly $USERDIR/monthly $USERDIR/weekly $USERDIR/daily
    chmod 666 $USERDIR/advanced $USERDIR/master $USERDIR/simple $USERDIR/cronlog.txt $USERDIR/systemupdate
    chown -R root.root $USERDIR
    if [ -f "$CRONSTOP" ]; then
        echo "Cron turned off with $CRONSTOP"
        exit
    fi
    CRONPID=`pgrep crond`
    if [ -z "$CRONPID" ]; then
        echo "Starting Cron Daemon"
        crond -b -c $CRONDIR $OPTS
    else
        echo "Cron already running as pid $CRONPID"
    fi
esac

# END

EOCD

chmod 775 /system/etc/init.d/20crond
chown root.root /system/etc/init.d/20crond

# This part is for backups
mkdir -p /data/media/0/ZooKeeper/snapshot
cat >/data/media/0/ZooKeeper/backup.sh <<"EOBACKUP"
#!/system/xbin/sh
#
# Backup Script
# /sdcard/ZooKeeper/backup.sh
# - the backup control script
#

PATH="/system/xbin:$PATH"
export PATH
cd /data/media/0/ZooKeeper
echo "Starting Backup!"
rm -rf snapshot
find /data/app -print | grep -F 'base.apk' | xargs -n 1 /data/media/0/ZooKeeper/cpAPK.sh
echo
echo "Backing Up Extra Files ..."
tar -cpJ -f /data/media/0/ZooKeeper/app-lib.tar.xz /data/app-lib 2>/dev/null
tar -cpJ -f /data/media/0/ZooKeeper/app-private.tar.xz /data/app-private 2>/dev/null
date >snapshot/full-backup.txt
echo "Backup Complete!"

EOBACKUP
chmod 777 /data/media/0/ZooKeeper/backup.sh

cat >/data/media/0/ZooKeeper/cpAPK.sh <<"EOCPAK"
#!/system/xbin/sh
#
# CpAPK
# /sdcard/ZooKeeper/cpAPK.sh
# - backs up one APK w/data
#

APK=$1
NAME=`echo $1 | cut -d '/' -f 4`
NEWN=`echo $NAME | cut -d '-' -f 1`
if [ ! -d "/data/media/0/ZooKeeper/snapshot/$NEWN" ]; then
        mkdir -p /data/media/0/ZooKeeper/snapshot/$NEWN
fi
echo "$NEWN"
tar -cpJ -f /data/media/0/ZooKeeper/snapshot/$NEWN/$NEWN.apk.tar.bz2 $APK 2>/dev/null
tar -cpJ -f /data/media/0/ZooKeeper/snapshot/$NEWN/$NEWN.data.tar.bz2 /data/data/$NEWN 2>/dev/null

EOCPAK
chmod 777 /data/media/0/ZooKeeper/cpAPK.sh

cat >/data/media/0/ZooKeeper/wipe-snapshot.sh <<"EOWS"
#!/usr/bin/env bash
#
# Wipe Snapshot
# /sdcard/ZooKeeper/wipe-snapshot.sh
# - Easily Delete Snapshot (run as root)
#

echo "Wiping ..."
rm -rf /data/media/0/ZooKeeper/snapshot
echo "Done!"

EOWS
chmod 777 /data/media/0/ZooKeeper/wipe-snapshot.sh

cat >/data/media/0/ZooKeeper/restore.sh <<"EOR"
#!/system/xbin/sh
#
# restore.sh
# /sdcard/ZooKeeper/restore.sh
# - run to restore all backups
#

APP=$1
DIR="/data/media/0/ZooKeeper/snapshot"

if [ -z "$APP" ]; then
        APP="*"
        if [ -f "$DIR/full-backup.txt" ]; then
            echo "Restoring all apps .. this will take time!"
        else
            echo "Not a full backup!  Aborting!"
            exit
        fi
else
        if [ -d "${DIR}/${APP}" ]; then
                echo "Restoring $APP"
        else
                echo "No Backup Found for $APP"
                exit 1
        fi
fi
PATH="/system/xbin:$PATH"
export PATH
cd "$DIR"
for dir in `echo $APP`; do
        cd "${DIR}/${dir}"
        OUT=`tar -vxf "${DIR}/${dir}/${dir}.apk.tar.bz2"`
        RESULT=`pm install -r -d "${DIR}/${dir}/$OUT" 2>&1`
        UID_CHANGE=`echo $RESULT | grep UID_CHANGED`
        if [ ! -z "$UID_CHANGE" ]; then
                echo UID_CHANGED
                rm -rf /data/data/$APP*
                pm install -r -d "${DIR}/${dir}/$OUT"
        else
                echo $APP - $RESULT
        fi
        rm -rf data
        cd /
        USERID=`dumpsys package $APP | grep userId= | sed -Er 's/.*userId=([0-9]+) .*/\1/'`
        if [ ! -d /data/data/$APP* ]; then
            tar -xf "/data/media/0/ZooKeeper/snapshot/${dir}/${dir}.data.tar.bz2"
        fi
        chown -R ${USERID}:${USERID} /data/data/$APP*
done
echo
echo "Restore Complete!"

EOR
chmod 777 /data/media/0/ZooKeeper/restore.sh
sync
mount -o remount,ro /system
sync
