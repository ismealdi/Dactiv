package com.ismealdi.dactiv.activity.kegiatan.detail

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.ismealdi.dactiv.R
import com.ismealdi.dactiv.base.AmActivity
import com.ismealdi.dactiv.model.Kegiatan
import com.ismealdi.dactiv.util.Constants.INTENT.DETAIL_KEGIATAN
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.android.synthetic.main.activity_kegiatan_detail.*
import kotlinx.android.synthetic.main.toolbar_primary.*
import android.text.format.DateFormat
import android.widget.LinearLayout
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.ismealdi.dactiv.App
import com.ismealdi.dactiv.activity.MessageActivity
import com.ismealdi.dactiv.adapter.UserAdapter
import com.ismealdi.dactiv.model.Alert
import com.ismealdi.dactiv.model.Attendent
import com.ismealdi.dactiv.model.User
import com.ismealdi.dactiv.util.*
import com.ismealdi.dactiv.util.barcode.BarcodeCaptureActivity
import com.ismealdi.dactiv.watcher.AmCurrencyWatcher
import net.glxn.qrgen.android.QRCode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Al on 23/10/2018
 */
class DetailKegiatanActivity : AmActivity(), DetailKegiatanContract.View {

    internal lateinit var mKegiatan : Kegiatan

    override lateinit var presenter: DetailKegiatanContract.Presenter
    override lateinit var progress: KProgressHUD
    override var mUsers : MutableList<User> = mutableListOf()
    override var mAttendents : MutableList<Attendent> = mutableListOf()

    private lateinit var mAdapter : UserAdapter
    private var format: NumberFormat? = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val request = 20
    private var detector: BarcodeDetector? = null
    private val capture = 9001

    private lateinit var datePicker: DatePickerDialog
    private var isDeskripsi = false


    fun init() {
        mKegiatan = intent.getParcelableExtra(DETAIL_KEGIATAN)

        progress = Dialogs.initProgressDialog(this)
        presenter = DetailKegiatanPresenter(this, this)

        presenter.users(mKegiatan.bagian)
        presenter.attendents(mKegiatan)

        initList()
        watcher()
        listener()

        setTitle(getString(R.string.title_kegiatan))
        buttonBackToolbar.visibility = View.VISIBLE
        buttonMenuToolbar.visibility = View.VISIBLE
        buttonMenuToolbar.setPadding(10,10,10,10)
        buttonMenuToolbar.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_qr_code))

        setData(mKegiatan)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initData(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kegiatan_detail)
        init()
    }

    private fun watcher() {
        textRealisasi.addTextChangedListener(AmCurrencyWatcher(textRealisasi))
    }

    private fun listener() {
        buttonBackToolbar.setOnClickListener {
            onBackPressed()
        }

        buttonMenuToolbar.setOnClickListener {
            doScanBarcode()
        }

        layoutJadwalPelaksana.setOnClickListener {
            initCalendarDialog()
            datePicker.show()
        }
    }

    override fun setData(kegiatan: Kegiatan) {
        textName.setTextFade(kegiatan.name)
        textKodeKegiatan.setTextFade(Utils.stringKodeFormat(kegiatan.kodeKegiatan))
        textAnggaran.setTextFade(format!!.format(kegiatan.anggaran))
        textDate.setTextFade(DateFormat.format("d MMMM yyyy hh:mm", kegiatan.jadwal).toString())

        val bitmap = QRCode.from(kegiatan.id).withSize(1000, 1000).bitmap()
        imageBarCode.setImageBitmap(bitmap)

        checkState()

        if(App.fireBaseAuth.currentUser != null) {

            if (kegiatan.penanggungJawab == App.fireBaseAuth.currentUser!!.uid && DateFormat.format("d MMMM yyyy", kegiatan.jadwal) == DateFormat.format("d MMMM yyyy", Calendar.getInstance())) {
                imageOverlay.visibility = View.GONE
            }

            if(kegiatan.status == 1) {
                layoutAdmin.visibility = View.VISIBLE
                layoutDeskripsi.isClickable = false
                layoutDeskripsi.isEnabled = false
            }else if(kegiatan.status == 4){
                textAlasan.visibility = View.VISIBLE
                textAlasan.setTextFade(kegiatan.alasan)
                val persentase = ((kegiatan.realisasi.toFloat() / kegiatan.anggaran.toFloat()) * 100)
                textAnggaran.setTextFade(format!!.format(kegiatan.realisasi) + " (${String.format("%2.02f", persentase)}%) ")
                textDate.setTextFade(DateFormat.format("d MMMM yyyy hh:mm", kegiatan.pelaksanaan).toString())
            }

            buttonAlarm.isEnabled = false
            buttonMessage.isEnabled = false
            presenter.penanggungJawab(kegiatan.penanggungJawab)

            mKegiatan = kegiatan
        }

    }

    private fun initList() {

        mAdapter = UserAdapter(mutableListOf(), mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(applicationContext,
                LinearLayout.VERTICAL, false)
        recyclerView.adapter = mAdapter

    }

    override fun populateAttendent(mUsers: MutableList<User>) {
        mAdapter.updateUser(mUsers)
    }

    private fun checkState() {
        if(mKegiatan.status != 1 || mKegiatan.attendent.any { x -> x.user == App.fireBaseAuth.currentUser!!.uid }) {
            buttonMenuToolbar.visibility = View.GONE
            layoutAdmin.visibility = View.GONE
            buttonAlarm.isEnabled = false
            buttonMessage.isEnabled = false

        }
    }

    private fun openMessage(user: User) {
        val mIntent = Intent(this, MessageActivity::class.java)

        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.SATKER, mKegiatan.satker)
        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.KEGIATAN, mKegiatan.id)
        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.KEGIATAN_NAMA, mKegiatan.name)
        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.MESSAGE, user.uid)
        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.NAME, user.displayName)
        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.DESCRIPTION, "")
        mIntent.putExtra(Constants.INTENT.LOGIN.PUSH.DATE, DateFormat.format("d MMMM yyyy h:m", Calendar.getInstance()).toString())

        startActivity(mIntent)
    }

    private fun doScanBarcode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                callBarcode()
            }else {
                ActivityCompat.requestPermissions(this@DetailKegiatanActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission_group.CAMERA, Manifest.permission.CAMERA), request)
            }
        }else{
            callBarcode()
        }
    }

    private fun callBarcode() {
        val intent = Intent(this, BarcodeCaptureActivity::class.java)
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true)
        startActivityForResult(intent, capture)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == request) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                callBarcode()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == capture) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    val barcode: Barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject)
                    if(barcode != null) {
                        presenter.doAttend(barcode.displayValue, mKegiatan)
                    }
                }
            } else {
                showSnackBar("Invalid error!")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun displayPenanggunJawab(user: User) {
        if(App.fireBaseAuth.currentUser != null) {
            buttonMessage.isEnabled = true
            textPenanggung.setTextFade(getString(R.string.text_admin) + ": ${user.displayName}")
            if (App.fireBaseAuth.currentUser!!.uid != user.uid) {
                buttonMessage.setOnClickListener {
                    openMessage(user)
                }
            }else{
                if(mKegiatan.status == 1) {
                    buttonMenuToolbar.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_checked))
                    buttonMenuToolbar.visibility = View.VISIBLE
                    buttonMenuToolbar.isEnabled = true
                    buttonMenuToolbar.setPadding(18, 18, 18, 18)

                    buttonMenuToolbar.setOnClickListener {
                        presenter.setAsDone(mKegiatan, textRealisasi.text.toString().toNumber(), textJadwalPelaksana.text.toString(), textDeskripsi.text.toString())
                    }
                }

                buttonMessage.setOnClickListener {
                    remindAll(mKegiatan.id, user)
                }
            }
        }
    }

    override fun onError(message: String) {
        showSnackBar(message)
    }

    override fun onDoneAttend() {
        checkState()
    }

    override fun onDoneKegiatan() {
        checkState()
    }

    private fun remindAll(kegiatan: String, penanggung: User) {
        val mMessage = Alert()

        mMessage.fromUser = penanggung.uid
        mMessage.bagian = mKegiatan.bagian
        mMessage.description = "Pengingat untuk semua anggota harap mengikuti \"${mKegiatan.name}\" yang akan dilaksanakan pada ${DateFormat.format("d MMMM yyyy h:m", mKegiatan.jadwal)}"
        mMessage.title = "Reminder"
        mMessage.date = penanggung.displayName
        mMessage.kegiatan = kegiatan

        presenter.remindTo(mMessage)
    }

    override fun reloadAttendent(mAttendents: MutableList<Attendent>) {
        this.mAttendents = mAttendents
        mAdapter.updateData(mAttendents)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.killSnapshot()
    }

    @SuppressLint("SetTextI18n")
    private fun initCalendarDialog() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = intent.getIntExtra(Constants.INTENT.SELECTED_DATE, c.get(Calendar.DAY_OF_MONTH))

        if(intent.getIntExtra(Constants.INTENT.SELECTED_DATE, 0) > 0) {
            textJadwalPelaksana.text = "$day/${month + 1}/$year ${c.get(Calendar.HOUR_OF_DAY)}:${c.get(Calendar.MINUTE)}"
        }

        val timePickerDialog = TimePickerDialog(this,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    textJadwalPelaksana.text = textJadwalPelaksana.text.toString() + " $hourOfDay:$minute"

                    val jad = SimpleDateFormat("dd/MM/yyyy").parse(textJadwalPelaksana.text.toString())
                    val dat = SimpleDateFormat("dd/MM/yyyy").parse(DateFormat.format("dd/MM/yyyy", mKegiatan.jadwal).toString())

                    if(dat.time < jad.time) {
                        setLayoutDeskripsi(true)
                    }
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)

        timePickerDialog.setOnCancelListener {
            textJadwalPelaksana.text = ""
            setLayoutDeskripsi(false)
        }

        datePicker = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, y, m, d ->
            val mm = m + 1
            textJadwalPelaksana.text =  "$d/$mm/$y"
            timePickerDialog.show()
        }, year, month, day)

        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000

        datePicker.setOnCancelListener {
            textJadwalPelaksana.text = ""
            setLayoutDeskripsi(false)
        }
    }

    private fun setLayoutDeskripsi(v: Boolean) {
        layoutDeskripsi.isClickable = v
        layoutDeskripsi.isEnabled = v
        textDeskripsi.isEnabled = v
        textDeskripsi.setText("")

        isDeskripsi = v
    }

}