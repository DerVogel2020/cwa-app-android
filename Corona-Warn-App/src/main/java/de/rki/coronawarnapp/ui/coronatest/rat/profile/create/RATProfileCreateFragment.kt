package de.rki.coronawarnapp.ui.coronatest.rat.profile.create

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import de.rki.coronawarnapp.R
import de.rki.coronawarnapp.databinding.RatProfileCreateFragmentBinding
import de.rki.coronawarnapp.util.di.AutoInject
import de.rki.coronawarnapp.util.ui.popBackStack
import de.rki.coronawarnapp.util.ui.viewBindingLazy
import de.rki.coronawarnapp.util.viewmodel.CWAViewModelFactoryProvider
import de.rki.coronawarnapp.util.viewmodel.cwaViewModels
import javax.inject.Inject

class RATProfileCreateFragment : Fragment(R.layout.rat_profile_create_fragment), AutoInject {
    @Inject lateinit var viewModelFactory: CWAViewModelFactoryProvider.Factory

    private val binding: RatProfileCreateFragmentBinding by viewBindingLazy()
    private val viewModel: RATProfileCreateFragmentViewModel by cwaViewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) =
        with(binding) {
            toolbar.setNavigationOnClickListener {
                popBackStack()
            }

            profileSaveButton.setOnClickListener {
                viewModel.saveProfile()
            }
        }
}