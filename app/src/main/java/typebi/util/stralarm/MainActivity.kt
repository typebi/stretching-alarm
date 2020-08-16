package typebi.util.stralarm

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.content_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var db:SQLiteDatabase
    private var existsAlarm = false
    private var closestAlarm :Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = openOrCreateDatabase("stretchingAlarm",MODE_PRIVATE, null)
        db.execSQL(getString(R.string.createTable))
        //registerReceiver(AlarmReceiver(), IntentFilter(Intent.ACTION_TIME_TICK))
        var isRunning = true
        class TimeChecker:Thread(){
            override fun run() {
                while (isRunning){
                    SystemClock.sleep(1000)
                    var displayMsg : String? = null
                    if (closestAlarm<0) {
                        displayMsg ="오늘 알람은 끝"
                    }else if(closestAlarm==Int.MAX_VALUE) {
                        displayMsg ="오늘 알람이 없어요"
                    }else{
                        val hour = closestAlarm/60/60
                        val min = closestAlarm/60%60
                        val sec = closestAlarm%60%60
                        displayMsg = if (hour>0) "다음 스트레칭까지\n"+reviseTime(hour)+" : "+reviseTime(min)+" : "+reviseTime(sec)
                        else "다음 스트레칭까지\n"+reviseTime(min)+" : "+reviseTime(sec)
                        closestAlarm--
                        if(closestAlarm<0) closestAlarm = checkClosest()
                    }
                    time_display.text = displayMsg
                }
            }
        }
        val timeChecker = TimeChecker()
        timeChecker.start()
        renewAlarms()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data!=null) {
            val isDelete = data.getBooleanExtra("isDelete",false)
            var num = data.getIntExtra("num", 0)
            if (!isDelete) {
                val name = data.getStringExtra("name")
                val sh = data.getIntExtra("startHour", 0)
                val sm = data.getIntExtra("startMin", 0)
                val eh = data.getIntExtra("endHour", 0)
                val em = data.getIntExtra("endMin", 0)
                val intvl = data.getIntExtra("intvl", 0)
                when (requestCode) {
                    1001 -> {
                        db.insert("STRALARM","SETTINGS", makeDataRow(name, sh, sm, eh, em, intvl))
                        val row = db.rawQuery(getString(R.string.selectLatest), null)
                        row.moveToNext()
                        num = row.getInt(0)
                        row.close()
                        //adsense.text = name + sh.toString()
                        alarm_list.removeView(alarm_list.children.last())
                        alarm_list.addView( makeNewAlarm(num, name, sh.toString(), reviseTime(sm), eh.toString(), reviseTime(em), intvl.toString() ) )
                        alarm_list.addView(addNewBtn())
                        closestAlarm = checkClosest()
                        existsAlarm = true
                    }
                    1002 -> {
                        db.update("STRALARM", makeDataRow(name, sh, sm, eh, em, intvl), "NUM = ?", arrayOf(num.toString()))
                        closestAlarm = checkClosest()
                        renewAlarms()
                    }
                }
            }else{
                db.execSQL("delete from STRALARM where num=$num")
                existsAlarm = false
                closestAlarm = checkClosest()
                renewAlarms()
            }
        }
    }
    private fun makeNewAlarm(num:Int, name:String?, sh:String, sm:String, eh:String, em:String,intvl:String): TextView{
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (110*resources.displayMetrics.density+0.5f).toInt()
        )
        val dp = (10*resources.displayMetrics.density+0.5f).toInt()
        params.setMargins(0,dp,0,dp)

        val alarm = Button(this)
        alarm.setBackgroundResource(R.drawable.border_layout)
        alarm.layoutParams = params
        alarm.gravity = Gravity.CENTER_VERTICAL
        //alarm.background = Color.WHITE.toDrawable()
        var content = "$sh:$sm ~ $eh:$em  / $intvl m\n월화수목금토일"
        if (name!=null && name.isNotEmpty()) content = "$name\n$content"
        alarm.text = content
        alarm.textSize = 25.toFloat()
        alarm.setTextColor(Color.parseColor("#000000"))
        alarm.id = num
        val intent = Intent(this, AddAlarm::class.java)
        intent.putExtra("num", alarm.id)
        intent.putExtra("name", name)
        intent.putExtra("sh", sh.toInt())
        intent.putExtra("sm", sm.toInt())
        intent.putExtra("eh", eh.toInt())
        intent.putExtra("em", em.toInt())
        intent.putExtra("intvl", intvl.toInt())
        alarm.setOnClickListener{
            startActivityForResult(intent, 1002)
        }
        return alarm
    }
    private fun addNewBtn() :ImageButton{
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (125*resources.displayMetrics.density+0.5f).toInt()
        )
        val dp = (10*resources.displayMetrics.density+0.5f).toInt()
        params.setMargins(0,dp,0,dp)
        val plusAlarmBtn = ImageButton(this)
        plusAlarmBtn.setBackgroundResource(R.drawable.border_layout)
        plusAlarmBtn.layoutParams = params
        plusAlarmBtn.setImageResource(android.R.drawable.ic_input_add)
        val intentFromNewbtn = Intent(this, AddAlarm::class.java)
        intentFromNewbtn.putExtra("isNew",true)
        plusAlarmBtn.setOnClickListener {
            startActivityForResult(intentFromNewbtn,1001)
        }
        return plusAlarmBtn
    }
    private fun renewAlarms(){
        alarm_list.removeAllViews()
        val alarms = db.rawQuery("select * from STRALARM", null)
        if(alarms.count!=0) {
            existsAlarm = true
            for (i in 1..alarms.count) {
                alarms.moveToNext()
                alarm_list.addView(
                    makeNewAlarm(
                        alarms.getInt(0),
                        alarms.getString(1),
                        alarms.getInt(2).toString(),
                        reviseTime(alarms.getInt(3)),
                        alarms.getInt(4).toString(),
                        reviseTime(alarms.getInt(5)),
                        alarms.getInt(6).toString()
                    )
                )
            }
        }else{
            existsAlarm=false
        }
        alarms.close()
        alarm_list.addView(addNewBtn())
    }
    private fun makeDataRow(name:String?, sh:Int, sm:Int, eh:Int, em:Int,intvl:Int) :ContentValues{
        val cv = ContentValues()
        cv.put("NAME", name)
        cv.put("START_H", sh)
        cv.put("START_M", sm)
        cv.put("END_H", eh)
        cv.put("END_M", em)
        cv.put("INTERVAL", intvl)
        return cv
    }
    private fun reviseTime(time:Int) :String{
        return if(time<10) "0$time"
        else time.toString()
    }
    private fun checkClosest() : Int {
        var closestTime = Int.MAX_VALUE
        val curH = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val curM = Calendar.getInstance().get(Calendar.MINUTE)
        val curS = Calendar.getInstance().get(Calendar.SECOND)
        val alarms = db.rawQuery("select * from STRALARM", null)
        if (alarms.count != 0) {
            for (i in 1..alarms.count) {
                alarms.moveToNext()
                var alarmTimeS = alarms.getInt(2)*60*60 + alarms.getInt(3)*60
                val curTimeS = curH*60*60 + curM*60 + curS
                val endTimeS = alarms.getInt(4)*60*60 + alarms.getInt(5)*60
                val interval = alarms.getInt(6)*60
                val am = getSystemService(ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(this, AlarmReceiver::class.java)
                alarmIntent.putExtra("num",alarms.getInt(0))
                val sender = PendingIntent.getBroadcast(this, alarms.getInt(0), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                while (alarmTimeS <= curTimeS && alarmTimeS<=endTimeS)
                    alarmTimeS += interval
                if (alarmTimeS > endTimeS) {
                    am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+(alarmTimeS - curTimeS)*1000, interval.toLong()*1000, sender)
                    continue
                }
                if (alarmTimeS - curTimeS in 0 until closestTime)
                    closestTime = alarmTimeS - curTimeS
                am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+(alarmTimeS - curTimeS)*1000, interval.toLong()*1000, sender)
                Log.v("##################", "알람등록"+alarms)
            }
            alarms.close()
            if(closestTime==Int.MAX_VALUE) return Int.MAX_VALUE
            return closestTime-1
        }else{
            alarms.close()
            return Int.MAX_VALUE
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        //unregisterReceiver(AlarmReceiver())
    }
}
