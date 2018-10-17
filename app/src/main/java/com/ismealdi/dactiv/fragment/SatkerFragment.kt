package com.ismealdi.dactiv.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ismealdi.dactiv.R
import com.ismealdi.dactiv.activity.MainActivity
import com.ismealdi.dactiv.activity.satker.AddSatkerActivity
import com.ismealdi.dactiv.adapter.SatkerAdapter
import com.ismealdi.dactiv.base.AmFragment
import com.ismealdi.dactiv.model.Satker
import com.ismealdi.dactiv.util.Constants
import kotlinx.android.synthetic.main.fragment_main.*


class SatkerFragment : AmFragment() {

    private lateinit var mActivity : MainActivity
    private lateinit var mAdapter : SatkerAdapter

    private var mSatkers : MutableList<Satker> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initGrid()
        listener()
    }

    private fun listener() {
        buttonAdd.setOnClickListener {
            startActivityForResult(Intent(context, AddSatkerActivity::class.java), Constants.INTENT.ACTIVITY.ADD_SATKER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_satker, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        mActivity = activity as MainActivity
    }

    private fun initGrid() {
        mActivity.showProgress()

        mAdapter = SatkerAdapter(mSatkers, this)

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = mAdapter

        mActivity.hideProgress()

        showEmpty((mAdapter.itemCount == 0))
    }

    private fun showEmpty(b: Boolean) {
        if(b) {
            if(layoutEmpty != null) layoutEmpty.visibility = View.VISIBLE
            if(recyclerView != null) recyclerView.visibility = View.GONE
        }else{
            if(layoutEmpty != null) layoutEmpty.visibility = View.GONE
            if(recyclerView != null) recyclerView.visibility = View.VISIBLE
        }
    }

    fun updateList(mSatkersNew: MutableList<Satker>) {
        mSatkers.clear()
        mSatkers.addAll(mSatkersNew)
        mAdapter.updateData(mSatkersNew)
        showEmpty((mSatkers.size == 0))
    }

}
