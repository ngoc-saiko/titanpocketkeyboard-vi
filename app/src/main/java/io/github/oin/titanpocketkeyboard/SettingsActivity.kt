package io.github.oin.titanpocketkeyboard

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.core.app.ActivityCompat
import android.Manifest

class SettingsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.settings_activity)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.settings, SettingsFragment())
				.commit()
		}
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		checkAndRequestPermission()
	}

	private fun checkAndRequestPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
			!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == 1) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this, "Microphone permission is required for speech-to-text", Toast.LENGTH_LONG).show()
			}
		}
	}

	class SettingsFragment : PreferenceFragmentCompat() {
		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.preferences, rootKey)
			val context = activity
			if(context != null) {
				findPreference<Preference>("Reset")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
					AlertDialog.Builder(context)
						.setTitle("Reset settings")
						.setMessage("Do you really want to reset all the settings to their default value?")
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
							val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
							editor.clear()
							editor.commit()
							setPreferencesFromResource(R.xml.preferences, rootKey)
						})
						.setNegativeButton(android.R.string.no, null)
						.show()
					true
				}
			}
		}
	}
}