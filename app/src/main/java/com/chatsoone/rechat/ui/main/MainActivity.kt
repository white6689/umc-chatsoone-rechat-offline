package com.chatsoone.rechat.ui.main

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.chatsoone.rechat.ApplicationClass
import com.chatsoone.rechat.ApplicationClass.Companion.ACT
import com.chatsoone.rechat.NotificationListener
import com.chatsoone.rechat.R
import com.chatsoone.rechat.data.entity.Folder
import com.chatsoone.rechat.data.entity.Icon
import com.chatsoone.rechat.data.local.AppDatabase
import com.chatsoone.rechat.databinding.ActivityMainBinding
import com.chatsoone.rechat.ui.ChatViewModel
import com.chatsoone.rechat.ui.LockViewModel
import com.chatsoone.rechat.ui.explain.ExplainActivity
import com.chatsoone.rechat.ui.main.blocklist.BlockListFragment
import com.chatsoone.rechat.ui.main.folder.MyFolderFragment
import com.chatsoone.rechat.ui.main.hiddenfolder.MyHiddenFolderFragment
import com.chatsoone.rechat.ui.main.home.HomeFragment
import com.chatsoone.rechat.ui.pattern.CreatePatternActivity
import com.chatsoone.rechat.ui.pattern.InputPatternActivity
import com.chatsoone.rechat.ui.setting.PrivacyInfoActivity
import com.chatsoone.rechat.util.getID
import com.chatsoone.rechat.util.permissionGrantred
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView

class MainActivity : NavigationView.OnNavigationItemSelectedListener,
    AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase

    private var userID = getID()
    private var iconList = ArrayList<Icon>()
    private var folderList = ArrayList<Folder>()
    private var permission: Boolean = true

    private val chatViewModel: ChatViewModel by viewModels()
    private val lockViewModel: LockViewModel by viewModels()

    // ??????
    lateinit var mAdview: AdView
    lateinit var adRequest: AdRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)!!
        initIcon()
        initFolder()
        lockViewModel.setMode(0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()

        initAds()
        initHiddenFolder()
        initBottomNavigationView()
        initDrawerLayout()
        initClickListener()
    }

    // ????????? ?????????
    private fun initIcon() {
        iconList = database.iconDao().getIconList() as ArrayList

        if (iconList.isEmpty()) {
            // ????????? ?????? ??????
            // database.iconDao().insert(Icon())
            // iconList = database.iconDao().getIconList() as ArrayList
        }
    }

    // ?????? ?????????
    private fun initFolder() {

        // ?????? ?????? ?????? ?????? ??????

        database.folderDao().getFolderList(userID).observe(this) {
            Log.d(ApplicationClass.ACT, "MAIN/folderList: $folderList")
            folderList = it as ArrayList<Folder>
        }
    }

    // ?????? ?????? ?????????
    private fun initHiddenFolder() {
        val spf = getSharedPreferences("lock_correct", 0)

        if (spf.getInt("correct", 0) == 1) replaceFragment(MyHiddenFolderFragment())
        else if (spf.getInt("correct", 0) == -1) replaceFragment(MyFolderFragment())
        else replaceFragment(BlockListFragment())
    }

    // ??????????????? ??????
    private fun replaceFragment(fragment: Fragment) {
        this.supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame_layout, fragment).commitAllowingStateLoss()
    }

    // ?????? ?????????
    private fun initAds() {
        MobileAds.initialize(this)
        val headerView = binding.mainNavigationView.getHeaderView(0)
        mAdview = headerView.findViewById<AdView>(R.id.adViews)
        adRequest = AdRequest.Builder().build()
        mAdview.loadAd(adRequest)
    }

    private fun initBottomNavigationView() {
        Log.d(ACT, "MAIN/initBottomNavigationView")

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_frame_layout, HomeFragment())
            .commitAllowingStateLoss()

        binding.mainLayout.mainBnv.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.main_bnv_home -> {
                    // ?????? ??????
                    replaceFragment(HomeFragment())
                    return@setOnItemSelectedListener true
                }

                R.id.main_bnv_block_list -> {
                    // ?????? ??????
                    replaceFragment(BlockListFragment())
                    return@setOnItemSelectedListener true
                }

                R.id.main_bnv_folder -> {
                    // ?????????
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frame_layout, MyFolderFragment())
                        .commitAllowingStateLoss()

                    return@setOnItemSelectedListener true
                }

                R.id.main_bnv_hidden_folder -> {
                    // ?????? ?????????
                    val lockSPF = getSharedPreferences("lock", 0)
                    val pattern = lockSPF.getString("pattern", "0")

                    // ?????? ?????? ??????
                    // 0: ?????? ?????? ????????? ???????????? ?????? ?????? ??????
                    // 1: ?????? ????????? ????????? -> ?????? ??????
                    // 2: ?????? ????????? ????????? -> ?????? ??????
                    // 3: ?????? ?????? ?????? ??????????????? ?????? ?????? ?????? ???
                    val modeSPF = getSharedPreferences("mode", 0)
                    val editor = modeSPF.edit()

                    // ???????????? 0??? ??????
                    editor.putInt("mode", 0)
                    editor.apply()

                    if (pattern.equals("0")) {   // ????????? ???????????? ?????? ?????? ?????? ?????? ?????? ????????????
                        Toast.makeText(this, "????????? ???????????? ?????? ????????????.\n????????? ??????????????????.", Toast.LENGTH_SHORT)
                            .show()
                        startActivity(Intent(this@MainActivity, CreatePatternActivity::class.java))
                    } else {
                        startActivity(Intent(this@MainActivity, InputPatternActivity::class.java))
                    }

                    // ????????? ???????????? ??????
                    // 1: ????????? ??????
                    // 2: ???????????? ?????? ??????
                    initHiddenFolder()
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    private fun initDrawerLayout() {
        binding.mainNavigationView.setNavigationItemSelectedListener(this)
        val menuItem = binding.mainNavigationView.menu.findItem(R.id.navi_setting_alarm_item)
        val drawerSwitch =
            menuItem.actionView.findViewById(R.id.main_drawer_alarm_switch) as SwitchCompat

        // ?????? ?????? ?????? ????????? ?????? ????????? ?????? ?????? ??????
        if (permissionGrantred(this)) {
            // ?????? ????????? ???????????? ?????? ??????
            drawerSwitch.toggle()
            drawerSwitch.isChecked = true
            permission = true
        } else {
            // ?????? ????????? ???????????? ?????? ?????? ??????
            drawerSwitch.isChecked = false
            permission = false
        }

        drawerSwitch.setOnClickListener {
            if (drawerSwitch.isChecked) {
                // ?????? ????????? ???????????? ???
                permission = true
                Log.d(ACT, "MAIN/drawerSwitch.isChecked == true")
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))

                if (permissionGrantred(this)) {
                    Toast.makeText(this, "?????? ????????? ???????????????.", Toast.LENGTH_SHORT).show()
                    Log.d(ACT, "MAIN/inPermission")
                    startService(Intent(this, NotificationListener::class.java))
                }
            } else {
                // ?????? ????????? ???????????? ????????? ???
                permission = false
                Log.d(ACT, "MAIN/drawerSwitch.isChecked == false")
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                if (!permissionGrantred(this)) {
                    stopService(Intent(this, NotificationListener::class.java))
                    Toast.makeText(this, "?????? ????????? ???????????? ????????????.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initClickListener() {
        // ?????? ???????????? ?????? ?????? ????????? ????????? ?????? ????????? ????????????
        binding.mainLayout.mainSettingMenuIv.setOnClickListener {
            if (!binding.mainDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                // ?????? ???????????? ???????????? ???
                mAdview.loadAd(adRequest)
                binding.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    // ?????? ?????? ?????? ??????????????? ???????????? ??????????????? ?????? ???????????? ??????
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // ?????? ??????
            R.id.navi_setting_alarm_item -> {
                Toast.makeText(this, "?????? ??????", Toast.LENGTH_SHORT).show()
            }

            // ?????? ????????????
            R.id.navi_setting_pattern_item -> {
                val lockSPF = getSharedPreferences("lock", 0)
                val pattern = lockSPF.getString("pattern", "0")

                // ??? ???????????? ?????? DB ?????? X
                // ?????? ?????? ??????
                // 0: ?????? ?????? ????????? ???????????? ?????? ?????? ??????
                // 1: ?????? ????????? ????????? -> ?????? ??????
                // 2: ?????? ????????? ????????? -> ?????? ??????
                // 3: ?????? ?????? ????????? ????????? -> ?????? ?????? ????????? ??????
                val modeSPF = getSharedPreferences("mode", 0)
                val editor = modeSPF.edit()
                editor.putInt("mode", 1)
                editor.apply()

                if (pattern.equals("0")) {   // ????????? ???????????? ?????? ?????? ?????? ?????? ?????? ????????????
                    val intent = Intent(this@MainActivity, CreatePatternActivity::class.java)
                    startActivity(intent)
                } else {    // ????????? ???????????? ?????? ?????? ?????? ???????????? (????????? ??????)
                    val intent = Intent(this@MainActivity, InputPatternActivity::class.java)
                    startActivity(intent)
                }
            }
            // ?????? ?????? ?????????
            R.id.navi_setting_helper_item -> {
                ApplicationClass.mSharedPreferences = getSharedPreferences("explain", MODE_PRIVATE)
                val editor = ApplicationClass.mSharedPreferences.edit()
                editor.putInt("explain_from_menu", 1)
                editor.apply()

                val intent = Intent(this, ExplainActivity::class.java)
                startActivity(intent)
            }

            // ???????????? ????????????
            R.id.navi_setting_privacy_item -> {
                val intent = Intent(this, PrivacyInfoActivity::class.java)
                startActivity(intent)
            }
        }
        return false
    }

    // ???????????? ???????????? ??? ?????? ?????? ????????? ??? ?????? ?????? ?????? ????????? ?????? ???????????? ??????
    override fun onBackPressed() {
        if (binding.mainDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.mainDrawerLayout.closeDrawers()
        } else if (chatViewModel.mode.value == 1) {
            chatViewModel.setMode(mode = 0)
        } else {
            super.onBackPressed()
        }
    }
}