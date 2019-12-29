package jp.techacademy.naoto.ichihashi.taskapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(context: Context): BaseAdapter() {
//    xmlリソースのView(画面部品)を取得するためのLayoutInflaterをプロパティ定義
    private val mLayoutInflater: LayoutInflater
//    アイテムを保持するtaskListをプロパティ定義
    var taskList = mutableListOf<Task>()

    init{
        this.mLayoutInflater = LayoutInflater.from(context)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view:View = convertView ?: mLayoutInflater.inflate(android.R.layout.simple_list_item_2,null)

        val textView1 = view.findViewById<TextView>(android.R.id.text1)
        val testView2 = view.findViewById<TextView>(android.R.id.text2)

        textView1.text = taskList[position].title

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.JAPANESE)
        val date = taskList[position].date
        testView2.text = simpleDateFormat.format(date)

        return view
    }

    override fun getItem(position: Int): Any {
        return taskList[position]
    }

    override fun getItemId(position: Int): Long {
        return taskList[position].id.toLong()
    }

    override fun getCount(): Int {
        return taskList.size
    }
}