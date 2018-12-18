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
package im.vector.activity

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager
import android.view.MenuItem
import im.vector.Matrix
import im.vector.R
import im.vector.fragments.VectorSettingsNotificationsTroubleshootFragment
import im.vector.fragments.VectorSettingsPreferencesFragment
import im.vector.util.PreferencesManager

/**
 * Displays the client settings.
 */
class VectorSettingsActivity : MXCActionBarActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, FragmentManager.OnBackStackChangedListener {


    private lateinit var vectorSettingsPreferencesFragment: VectorSettingsPreferencesFragment

    override fun getLayoutRes(): Int {
        return R.layout.activity_vector_settings
    }

    override fun getTitleRes(): Int {
        return R.string.title_activity_settings
    }

    override fun initUiAndData() {
        configureToolbar()

        var session = getSession(intent)

        if (null == session) {
            session = Matrix.getInstance(this).defaultSession
        }

        if (session == null) {
            finish()
            return
        }

        if (isFirstCreation()) {
            vectorSettingsPreferencesFragment = VectorSettingsPreferencesFragment.newInstance(session.myUserId)
            // display the fragment
            supportFragmentManager.beginTransaction()
                    .replace(R.id.vector_settings_page, vectorSettingsPreferencesFragment, FRAGMENT_TAG)
                    .commit()
        } else {
            vectorSettingsPreferencesFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as VectorSettingsPreferencesFragment
        }


        supportFragmentManager.addOnBackStackChangedListener(this)

    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()
    }

    override fun onBackStackChanged() {
        if (0 == supportFragmentManager.backStackEntryCount) {
            supportActionBar?.title = getString(getTitleRes())
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {

        var session = getSession(intent)

        if (null == session) {
            session = Matrix.getInstance(this).defaultSession
        }

        if (session == null) {
            return false;
        }

        var oFragment: Fragment? = null

        if (PreferencesManager.SETTINGS_NOTIFICATION_TROUBLESHOOT_PREFERENCE_KEY == pref?.key) {
            oFragment = VectorSettingsNotificationsTroubleshootFragment.newInstance(session.myUserId)
        }

        if (oFragment != null) {
            oFragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.abc_popup_enter, R.anim.abc_popup_exit)
                    .replace(R.id.vector_settings_page, oFragment, pref?.title.toString())
                    .addToBackStack(null)
                    .commit()
            return true
        }

        return false;
    }

    companion object {
        @JvmStatic
        fun getIntent(context: Context, userId: String) = Intent(context, VectorSettingsActivity::class.java)
                .apply {
                    putExtra(MXCActionBarActivity.EXTRA_MATRIX_ID, userId)
                }

        private const val FRAGMENT_TAG = "VectorSettingsPreferencesFragment"
    }
}
