package jp.techacademy.naoto.ichihashi.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_input.*
import java.util.*

class InputActivity : AppCompatActivity() {
    //MainActivityからIntentで渡されるKEYを定義
    companion object{
        val KEY = "key"
    }

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask : Task ? = null

    private val mOnDateClickListener = View.OnClickListener{
        val datePickerDialog = DatePickerDialog(this,
        DatePickerDialog.OnDateSetListener{_,year,month,dayOfMonth ->
            mYear = year
            mMonth = month
            mDay = dayOfMonth
            val dateString = mYear.toString() + "/" + String.format("%02d",mMonth + 1)+"/" + String.format("%02d",mDay)
            date_button.text = dateString},mYear,mMonth,mDay)
        datePickerDialog.show()
    }

    private val mOnTimeClickListener = View.OnClickListener{
        val timePickerDialog = TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener{_,hour,minute ->
                mHour = hour
                mMinute = minute
                val timeString = String.format("%02d",mHour) + ":" + String.format("%02d",mMinute)
                times_button.text = timeString
            },mHour,mMinute,true)
        timePickerDialog.show()
    }

    private val mOnDoneClickListener = View.OnClickListener{
        addTask()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        //ActionBarの設定
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if(supportActionBar != null){
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        //UI部品の設定
        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimeClickListener)
        done_button.setOnClickListener(mOnDoneClickListener)

        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK,-1)
        val realm = Realm.getDefaultInstance()
        mTask = realm.where(Task::class.java).equalTo("id",taskId).findFirst()
        realm.close()

        if(mTask == null){
            //新規作成の場合の処理
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)
        }else{
            //更新の場合の処理
            //EditTextにmTaskにすでに登録されている内容を設定
            category_edit_text.setText(mTask!!.category)
            title_edit_text.setText(mTask!!.title)
            content_edit_text.setText(mTask!!.contents)

            val calendar = Calendar.getInstance()
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val dateString = mYear.toString() + "/" + String.format("%02d",mMonth + 1) + "/" + String.format("%02d",mDay)
            val timeString = String.format("%02d",mHour) + ":" + String.format("%02d",mMinute)

            date_button.text = dateString
            times_button.text = timeString
            }
        //spinner_button1クリック時にspinnerを生成してクリック処理を実行
        spinner_button1.setOnClickListener{view ->
            spinnerCreate()
            spinner1.performClick()
        }
    }


    private fun addTask(){
        //Realmオブジェクトを取得
        val realm = Realm.getDefaultInstance()

        //Realmの処理開始
        realm.beginTransaction()

        if(mTask == null){
            //新規作成の場合の処理
            mTask = Task()
            //Taskクラスの生成とid値の設定
            val taskRealmResults = realm.where(Task::class.java).findAll()
            val identifier:Int =
                if (taskRealmResults.max("id") != null){
                    //保存済みタスクidが存在する場合、最大値に1を足した値をid値に設定
                    taskRealmResults.max("id")!!.toInt() + 1
                }else{
                    //保存済みタスクがない(null)の場合、0に設定
                    0
                }
            mTask!!.id = identifier
        }

        //EditTextへの入力をString型で取得
        val category = category_edit_text.text.toString()
        val title = title_edit_text.text.toString()
        val content = content_edit_text.text.toString()

        //取得したEditTextのデータをmTaskに登録
        mTask!!.category = category
        mTask!!.title = title
        mTask!!.contents = content
        //日時をmTaskに登録
        val calendar = GregorianCalendar(mYear,mMonth,mDay,mHour,mMinute)
        val date = calendar.time
        mTask!!.date = date

        //Realmデータを更新
        realm.copyToRealmOrUpdate(mTask!!)
        //Realm処理確定
        realm.commitTransaction()
        //Realm終了

        realm.close()

        //TaskAlarmReceiverを起動するIntentオブジェクトを取得
        val resultIntent = Intent(applicationContext,TaskAlarmReceiver::class.java)
        //IntentオブジェクトにEXTRA_TASKとしてmTaskのタスクid値を登録
        resultIntent.putExtra(EXTRA_TASK,mTask!!.id)
        //PendingIntentオブジェクトを取得
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        //AlarmManagerオブジェクトを取得
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,resultPendingIntent)
    }


    private fun spinnerCreate(){
        //Intentから渡されるstateをキーワードKEYを指定し、Serializableにて受け取る。
        val state = intent.getSerializableExtra(KEY)
        //受け取ったstateを利用した処理を以下のif文内に記述
        //下記if文がないとエラーになる。理由は不明。2019.12.29時点。
        if(state is MainActivity.DataState){
        //stateの中身確認のログ出力
        Log.d("CAT",state.list.toString())
        //アダプタオブジェクトの生成。stateを.listでMutableList型に、更に.toList()でList型に変換してアダプタに登録
        var adapter = ArrayAdapter(applicationContext, R.layout.custom_spinner,state.list.toList())
        //spinnerアダプタ設定、独自レイアウトcustom_spinner、リストcategory_list
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
        spinner1.adapter = adapter
        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
                val spinnerParent = parent as Spinner
                //選択された項目の文字列をitemに取得
                val item = spinnerParent.selectedItem as String
                //EditTextのTextとして選択した文字列を設定
                category_edit_text.setText(item)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

}

