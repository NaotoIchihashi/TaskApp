package jp.techacademy.naoto.ichihashi.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort

import kotlinx.android.synthetic.main.activity_main.*
import java.io.Serializable
import java.util.*

const val EXTRA_TASK = "jp.techacademy.naoto.ichihashi.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    //Realmを保持するmRealmをプロパティ定義
    private lateinit var mRealm: Realm
    //カテゴリ管理用の配列を用意.set型を使うことで重複を回避
    private var category_list = mutableSetOf<String?>()

    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    //TaskAdapterを保持するプロパティを定義する
    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            //category_listオブジェクトをIntentで渡すためDataStateメソッドにてSerializableに変換
            val state = DataState(category_list.toMutableList())
            //IntentにKEYとstateを紐づけて設定
            intent.putExtra(InputActivity.KEY,state)
            startActivity(intent)
        }

        //Realmのオブジェクト取得
        mRealm = Realm.getDefaultInstance()
        //mRealmListenerをmRealmに設定
        mRealm.addChangeListener(mRealmListener)

        //ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //ListViewをタップした時の処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            //タップした位置のタスクidを取得
            val task = parent.adapter.getItem(position) as Task
            //Intentオブジェクトを取得
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            //IntentオブジェクトにEXTRA_TASKデータとしてタスクidを登録
            intent.putExtra(EXTRA_TASK, task.id)
            //category_listオブジェクトをIntentで渡すためDataStateメソッドにてSerializableに変換
            val state = DataState(category_list.toMutableList())
            //IntentにKEYとstateを紐づけて設定
            intent.putExtra(InputActivity.KEY,state)
            startActivity(intent)
        }

        //ListViewを長押ししたときに選択したタスクを削除する処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
        //選択したタスクのオブジェクトを取得
        val task = parent.adapter.getItem(position) as Task
        //ダイアログのオブジェクトを取得
        val builder = AlertDialog.Builder(this@MainActivity)
        //ダイアログの表示内容を設定
        builder.setTitle("削除")
        builder.setMessage(task.title + "を削除しますか")
        builder.setPositiveButton("OK") { _, _ ->
            val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()
            mRealm.beginTransaction()
            results.deleteAllFromRealm()
            mRealm.commitTransaction()

            val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
            val resultPendingIntent = PendingIntent.getBroadcast(this@MainActivity,task.id,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(resultPendingIntent)

            reloadListView()
            //category_listを更新
            categoryList()
            }
            //CANCELの場合は何もしない
            builder.setNegativeButton("CANCEL", null)
            //builder2のオブジェクトを取得
            val dialog = builder.create()
            //dialogを表示
            dialog.show()
        true
        }

        //全件削除ボタンを押した時の処理
        //ユーザに時々全件削除してもらえればid値が無限に増えるのを防げる
        //ただし、category_listも全削除されるので、カテゴリ作成が再度必要になってしまう
        //categoryはやはり別クラスでrealmデータベース作成した方が良いかも知れない。
        delete_all_button.setOnClickListener{ view ->

            val builder2 = AlertDialog.Builder(this@MainActivity)
            //ダイアログの表示内容を設定
            builder2.setTitle("全件削除")
            builder2.setMessage("実行してよろしいですか？")
            builder2.setPositiveButton("OK") { _, _ ->
                //Taskデータを全て消去。beginTransactionとcommitTransactionで囲む必要あり。
                mRealm.beginTransaction()
                mRealm.deleteAll()
                mRealm.commitTransaction()

                reloadListView()
                //category_listを更新
                categoryList()
            }
            //CANCELの場合は何もしない
            builder2.setNegativeButton("CANCEL", null)
            //builder2のオブジェクトを取得
            val dialog = builder2.create()
            //dialogを表示
            dialog.show()
            true
        }

        //全件表示ボタンを押したら時の処理
        category_all_button.setOnClickListener{
        reloadListView()
        }

        //spinner_buttonを押した時にspinnerを生成して更新すると同時にspinnerをクリック処理にて実行
        //spinnerは生成された時点で先頭の要素が自動的に選択状態となる仕様
        //ユーザが意図しない項目選択がされるを防ぐため
        //ユーザに起動されるまではspinnerを生成しないようにする
        //また、見栄えのためspinner欄はユーザに見えないようにして、ドロップダウンリストのみ見えるようにxmlを設計
        spinner_button.setOnClickListener{ view ->
            //category_listを更新
            categoryList()
            //spinnerを生成
            spinnerCreate()
            //spinnerをクリック処理して実行しドロップダウンメニューを表示
            spinner0.performClick()
        }
    }

    //MainActivityが起動、再起動する度にcategory_list更新とListViewの更新を行う。
    //こうすることでIntentでcategory_listを渡す際の更新漏れを防ぐ
    override fun onStart() {
        super.onStart()
        categoryList()
        reloadListView()
    }

    private fun reloadListView() {
//        Realmデータベースから「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults =
            mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
//        上記の結果をTaskListとしてコピーする
//        Realmのデータベースから取得したデータをAdapterで使う場合は一旦コピーしてから渡す
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)
//        TaskのListView用のアダプタにコピーしたデータをデータを渡す
        listView1.adapter = mTaskAdapter
//        表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    private fun addTaskForTest() {
        val task = Task()
        task.category = "サンプル"
        task.title = "サンプル"
        task.contents = "サンプル"
        task.date = Date()
        task.id = 0
        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(task)
        mRealm.commitTransaction()
    }

    private fun categoryList() {
        category_list.clear()
        //Realmのデータ取得
        var results2 = mRealm.where(Task::class.java).findAll()
        //インストール直後などRealmデータ数が0の場合は処理を実行しない
        if(results2.count() > 0){
            category_list.clear()
           //Realmのid最大値を取得
           var identifier2: Int = results2.max("id")!!.toInt()
            Log.d("CAT", identifier2.toString())

            var i = 0
            //全てのTaskデータのcategory要素を順番に取得し、category_listに格納
            do {
                //id値がiのTaskデータの最初の値categoryを取得
                var results3 = mRealm.where(Task::class.java).equalTo("id", i).findFirst()
                //category_listに取得したcategoryを追加
                //InputActivityでid値を連番で作成しているので、削除されたid値のデータがnullになり、category_listにもnullが追加されてしまう
                //この方法だと毎回すべてのTaskデータをサーチする必要があるので、データが大量にあると重くなるかもしれない。
                category_list.add(results3?.category)
                i = i + 1
                } while (i <= identifier2)
            //category_listのnull削除処理前のログ出力確認
            Log.d("CAT", "null削除前" + category_list.toString())
        } else {
            return
        }
        //category_listに含まれるnullを一括削除。これをspinnerのアダプタ生成時に渡すことでエラー回避が可能。
        //Collections.singleton()：指定したオブジェクトだけを含む不変のsetを返す。ここではnullのみを含むsetクラスを返す。
        category_list.removeAll(Collections.singleton(null))
        Log.d("CAT", "null削除後" + category_list.toString())
    }

    private fun spinnerCreate(){
        //spinnerアダプタ設定、独自レイアウトcustom_spinner、リストcategory_list
        var adapter = ArrayAdapter(applicationContext, R.layout.custom_spinner,category_list.toList())
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
        spinner0.adapter = adapter
        spinner0.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?,position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                //選択された項目の文字列をitemに取得
                val item = spinnerParent.selectedItem as String
                //Realmデータベースからcategory==itemとなるデータを取得
                val taskRealmResults =
                    mRealm.where(Task::class.java).equalTo("category", item).findAll()
                //上記の結果をTaskListとしてコピーする
                //Realmのデータベースから取得したデータをAdapterで使う場合は一旦コピーしてから渡す
                mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)
                //TaskのListView用のアダプタにコピーしたデータをデータを渡す
                listView1.adapter = mTaskAdapter
                //表示を更新するために、アダプターにデータが変更されたことを知らせる
                mTaskAdapter.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    //Intentでオブジェクトを引き渡すためのDataStateクラス準備
    //SerializableでオブジェクトをIntentで引き渡し可能な文字列に変換する
    data class DataState(
        var list :MutableList<String?>
    ):Serializable

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

}






////        カテゴリ検索ボタンを押した時の処理
//        category_search_button.setOnClickListener { view ->
//            val category_word = category_edit_text.text.toString()
//
//            if (category_word == "") {
//                val toast = Toast.makeText(this, R.string.msg, Toast.LENGTH_SHORT).show()
//            } else {
////        Realmデータベースからcategory==category_wordとなるデータを取得
//                val taskRealmResults =
//                    mRealm.where(Task::class.java).equalTo("category", category_word).findAll()
////        上記の結果をTaskListとしてコピーする
////        Realmのデータベースから取得したデータをAdapterで使う場合は一旦コピーしてから渡す
//                mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)
////        TaskのListView用のアダプタにコピーしたデータをデータを渡す
//                listView1.adapter = mTaskAdapter
////        表示を更新するために、アダプターにデータが変更されたことを知らせる
//                mTaskAdapter.notifyDataSetChanged()
//            }
//        }
