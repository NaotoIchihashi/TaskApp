package jp.techacademy.naoto.ichihashi.taskapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import io.realm.Realm

class TaskAlarmReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= 26){
            val channel = NotificationChannel("default",
                "Channel name",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Channel description"
            notificationManager.createNotificationChannel(channel)
        }

//        通知の設定を行う
        val builder = NotificationCompat.Builder(context,"default")
        builder.setSmallIcon(R.drawable.small_icon)
        builder.setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.large_icon))
        builder.setWhen(System.currentTimeMillis())
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setAutoCancel(true)

//        EXTRA_TASKからタスクid値を取得
        val taskId = intent!!.getIntExtra(EXTRA_TASK,-1)
//        Realmオブジェクトを取得
        val realm = Realm.getDefaultInstance()
//        Realmのtaskid値に該当するタスクデータを取得
        val task = realm.where(Task::class.java).equalTo("id",taskId).findFirst()

//        タスクデータの中身（タスク名、タイトル、内容）をそれぞれ設定
        builder.setTicker(task!!.title)
        builder.setContentText(task.category)
        builder.setContentTitle(task.title)
        builder.setContentText(task.contents)

//        通知をタップしたらアプリを起動するようにする
//        MainActivityオブジェクトを取得
        val startAppIntent = Intent(context,MainActivity::class.java)
//        IntentにFlag属性FLAG_ACTIVITY_BROUGHT_TO_FRONTを追加する
//        Flag属性はIntentの挙動を制御に利用
//        FLAG_ACTIVITY_BROUGHT_TO__FRONT：Flagで起動したActivityが
//        すでに存在する場合、既存のActivityを前面に出す（二重起動防止）
        startAppIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        val pendingIntent = PendingIntent.getActivity(context,0,startAppIntent,0)
        builder.setContentIntent(pendingIntent)

        notificationManager.notify(task!!.id,builder.build())
        realm.close()

        Log.d("TaskApp","onReceive")
    }
}