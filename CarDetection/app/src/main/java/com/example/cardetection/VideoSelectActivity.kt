package com.example.cardetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cardetection.R
import com.example.cardetection.entity.impl.VideoMediaEntity
import com.ucard.timeory.adapter.LocalVideoAdapter
import com.ucard.timeory.utils.MediaUtils
import kotlinx.android.synthetic.main.activity_video_select.*
import java.lang.ref.WeakReference

class VideoSelectActivity : AppCompatActivity(), LocalVideoAdapter.OnLocalVideoSelectListener {

    companion object {
        const val GET_LOCAL_VIDEOS: Int = 100
        /**
         * by moosphon on 2018/09/16
         * desc: 解决handler内存泄漏的问题，消息的处理需要放在内部类的{@link #Handler.handleMessage}
         */
        private class WithoutLeakHandler( mActivity: VideoSelectActivity) : Handler(){
            private var weakReference: WeakReference<VideoSelectActivity> = WeakReference(mActivity)

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when(msg.what){
                    GET_LOCAL_VIDEOS -> {
                        val activity = weakReference.get()

                        if (activity != null){
                            activity.adapter.setData(activity.videoData!!)
                            activity.rv_video.adapter = activity.adapter

                        }

                    }
                }
            }
        }
    }
    private var videoData: List<VideoMediaEntity>? = ArrayList()
    private var handler: Handler = WithoutLeakHandler(this)
    private val adapter = LocalVideoAdapter()
    private var currentVideo: String? = null
    private var currentVideoTitle: String? = null
    private var currentVideoSize: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_select)
        initView()
        btn_video_select.setOnClickListener {
            var intent = Intent(this@VideoSelectActivity,ProcessActivity::class.java)
            var bundle = Bundle()
            bundle.putString("activity", this.localClassName)
            bundle.putString("path",currentVideo)
            bundle.putString("title",currentVideoTitle)
            bundle.putString("size", currentVideoSize)
            intent.putExtras(bundle)
            Log.e("VideoSelectActivity", "将视频路径传递给processActivity")
            startActivity(intent)
        }
    }

    private fun initView() {
        setSupportActionBar(tb_video)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        tb_video.setNavigationOnClickListener {
            onBackPressed()
        }
        getPermission()
        rv_video.layoutManager = GridLayoutManager(this, 3)
        rv_video.addItemDecoration(MediaItemDecoration(8, 3))
        adapter.listener = this

    }

    /** 获取存储权限 */
    private fun getPermission() {
        if (Build.VERSION.SDK_INT>22){
            if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        arrayOf( Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 111)
            }else {
                //已经获取到存储权限了
                searchForLocalVideos()
            }
        }else {
            //这个说明系统版本在6.0之下，不需要动态获取权限。
            searchForLocalVideos()
        }

    }

    /**
     * by moosphon on 2018/09/15
     * desc: 搜索系统本地所有视频
     * use ContentResolver in {@link #MediaStore.Video} <br/>
     */
    private fun searchForLocalVideos(){
        Thread(Runnable {
            videoData = MediaUtils.getLocalVideos(this)
            Log.e("VideoSelectActivity", "扫描本地视频的数量为->"+videoData?.size)
            val message= Message()
            message.what = GET_LOCAL_VIDEOS
            handler.sendMessage(message)
        }).start()
    }


    override fun onDestroy() {
        super.onDestroy()
        /** 消除内存泄漏隐患 */
        handler.removeCallbacksAndMessages(null)
    }

    override fun onVideoSelect(view: View, position: Int) {
        currentVideo = videoData!![position].path
        currentVideoTitle = videoData!![position].title
        currentVideoSize = videoData!![position].size
        Log.e("VideoSelectActivity", "当前选中的视频为->$currentVideo")
    }


}
