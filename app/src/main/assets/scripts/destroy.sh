#!/system/bin/sh
#!/system/bin/sh
mount -o remount,rw /system
rm -rf /system/etc/cron*
rm -f /system/etc/init.d/20crond
rm -rf /data/media/0/Cron
rm -rf /data/media/0/ZooKeeper/Cron
rm -f /data/media/0/ZooKeeper/*
mount -o remount,ro /system

