#!/system/bin/sh
#
# All scripts and other info is stored on the Internal Storage in Cron
# Busybox sees these in Unix locations via the following symlinks
# The "root" crontab is the "master" file, created dynamically and
# then the user's "advanced" additions (if any) are appended.
#
USERDIR="/data/media/0/Cron"
SUBDIRS="hourly daily weekly monthly"
mount -o remount,rw /system
echo "root:x:0:0::/system/etc/crontabs:/system/bin/sh" > /system/etc/passwd
mkdir /system/etc/crontabs
chown root.root /system/etc/crontabs
chmod 775 /system/etc/crontabs
mkdir $USERDIR
chown root.root $USERDIR
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
ZooNotify "Running $DIR crontab" "file:///sdcard/Cron/cronlog.txt"
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
chmod 666 $USERDIR/advanced
chown root.root $USERDIR/advanced

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
if [ -z "$3" ]; then
    am start -n "systems.eddon.android.zoo_keeper/.Notify" --es Message "$1" --es URL "$2"
else
    am start -n "systems.eddon.android.zoo_keeper/.Notify" --es Message "$1" --es URL "$2" --es Type "$3"
fi
echo "$1"

EONTF

chmod 775 /system/xbin/ZooToast
chmod 775 /system/xbin/ZooNotify

# The magic link!
ln -s $USERDIR/master /system/etc/crontabs/root

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

CRONSTOP="/storage/emulated/0/Cron/.cronstop"
USERDIR="/data/media/0/Cron"
CRONDIR="/system/etc/crontabs"
# OPTS=`cat /system/etc/cronopts`
OPTS="-L /storage/emulated/0/Cron/cronlog.txt"

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
mount -o remount,ro /system
