/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.fragments

import android.content.Intent
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.BindView
import im.vector.Matrix
import im.vector.R
import im.vector.activity.MXCActionBarActivity
import im.vector.extensions.withArgs
import im.vector.fragments.troubleshoot.ANotificationTroubleshootTestManager
import im.vector.fragments.troubleshoot.TroubleshootTest
import im.vector.push.fcm.NotificationTroubleshootTestManager
import im.vector.util.BugReporter
import org.matrix.androidsdk.MXSession

class VectorSettingsNotificationsTroubleshootFragment : VectorBaseFragment() {

    @BindView(R.id.troubleshoot_test_recycler_view)
    lateinit var mRecyclerView: RecyclerView
    @BindView(R.id.bottomView)
    lateinit var mBottomView: ViewGroup
    @BindView(R.id.summ_title)
    lateinit var mSummaryTitle: TextView
    @BindView(R.id.summ_description)
    lateinit var mSummaryDescription: TextView
    @BindView(R.id.summ_button)
    lateinit var mSummaryButton: Button
    @BindView(R.id.runButton)
    lateinit var mRunButton: Button

    var testManager: NotificationTroubleshootTestManager? = null
    // members
    private var mSession: MXSession? = null

    override fun getLayoutResId() = R.layout.fragment_settings_notifications_troubleshoot


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContext = activity!!.applicationContext
        // retrieve the arguments
        val sessionArg = Matrix.getInstance(appContext).getSession(arguments!!.getString(MXCActionBarActivity.EXTRA_MATRIX_ID))
        mSession = sessionArg

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        mRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(mRecyclerView.context,
                layoutManager.orientation);
        mRecyclerView.addItemDecoration(dividerItemDecoration)


        mSummaryButton.setOnClickListener {
            BugReporter.sendBugReport()
        }

        mRunButton.setOnClickListener() {
            testManager?.retry()
        }
        startUI()
    }

    private fun startUI() {

        mSummaryTitle.text = getString(R.string.settings_troubleshoot_diagnostic)
        mSummaryDescription.text = getString(R.string.settings_troubleshoot_diagnostic_running_status,
                0,0)
        mSummaryButton.text = getText(R.string.send_bug_report)

        testManager = NotificationTroubleshootTestManager(this, mSession)

        testManager?.statusListener = {
            if (isAdded) {
                TransitionManager.beginDelayedTransition(mBottomView)
                when (it.diagStatus) {
                    TroubleshootTest.TestStatus.NOT_STARTED -> {
                        mSummaryDescription.text = ""
                        mSummaryButton.visibility = View.GONE
                        mRunButton.visibility = View.VISIBLE
                    }
                    TroubleshootTest.TestStatus.RUNNING -> {
                        mSummaryDescription.text = getString(
                                R.string.settings_troubleshoot_diagnostic_running_status,
                                it.currentTestIndex,
                                it.testList.size
                        )
                        mSummaryButton.visibility = View.GONE
                        mRunButton.visibility = View.GONE
                    }
                    TroubleshootTest.TestStatus.FAILED -> {
                        //check if there are quick fixes
                        var hasQuickFix = false
                        testManager?.testList?.let {
                            for (test in it) {
                                if (test.status == TroubleshootTest.TestStatus.FAILED && test.quickFix != null) {
                                    hasQuickFix = true
                                    break
                                }
                            }
                        }
                        if (hasQuickFix) {
                            mSummaryDescription.text = getString(R.string.settings_troubleshoot_diagnostic_failure_status_with_quickfix)
                        } else {
                            mSummaryDescription.text = getString(R.string.settings_troubleshoot_diagnostic_failure_status_no_quickfix)
                        }
                        mSummaryButton.visibility = View.VISIBLE
                        mRunButton.visibility = View.VISIBLE
                    }
                    TroubleshootTest.TestStatus.SUCCESS -> {
                        mSummaryDescription.text = getString(R.string.settings_troubleshoot_diagnostic_success_status)
                        mSummaryButton.visibility = View.VISIBLE
                        mRunButton.visibility = View.VISIBLE
                    }
                }
            }

        }
        mRecyclerView.adapter = testManager?.adapter
        testManager?.runDiagnostic()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ANotificationTroubleshootTestManager.REQ_CODE_FIX) {
            testManager?.retry()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDetach() {
        testManager?.cancel()
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MXCActionBarActivity)?.supportActionBar?.setTitle(R.string.settings_notification_troubleshoot)
    }

    companion object {
        private val LOG_TAG = VectorSettingsNotificationsTroubleshootFragment::class.java.simpleName
        // static constructor
        fun newInstance(matrixId: String) = VectorSettingsNotificationsTroubleshootFragment()
                .withArgs {
                    putString(MXCActionBarActivity.EXTRA_MATRIX_ID, matrixId)
                }
    }
}