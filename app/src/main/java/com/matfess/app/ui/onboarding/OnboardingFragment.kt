package com.matfess.app.ui.onboarding

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.matfess.app.R
import com.matfess.app.databinding.FragmentOnboardingBinding
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.Palette

class OnboardingFragment : BaseFragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val page = pageFromArguments() ?: return
        binding.stepLabel.text = page.eyebrow
        binding.title.text = page.title
        binding.description.text = page.description
        binding.highlights.text = page.highlights
        binding.icon.setImageResource(page.icon)
        val accent = Palette.accent(requireContext(), page.accent)
        binding.icon.setColorFilter(accent)
        binding.iconWrap.backgroundTintList = ColorStateList.valueOf(
            Palette.withAlpha(accent, 36)
        )
        binding.iconWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_soft)
        binding.iconWrap.backgroundTintList = ColorStateList.valueOf(Palette.withAlpha(accent, 36))
        binding.decoIcon1.backgroundTintList = ColorStateList.valueOf(Palette.withAlpha(accent, 26))
        binding.decoIcon1.setColorFilter(accent)
        binding.decoIcon2.backgroundTintList = ColorStateList.valueOf(Palette.withAlpha(accent, 26))
        binding.decoIcon2.setColorFilter(accent)
        binding.stepLabel.setTextColor(accent)

        binding.iconWrap.alpha = 0f
        binding.iconWrap.scaleX = 0.7f
        binding.iconWrap.scaleY = 0.7f
        binding.iconWrap.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(560).setStartDelay(80).start()
        binding.title.alpha = 0f
        binding.title.translationY = 16f
        binding.title.animate().alpha(1f).translationY(0f)
            .setDuration(460).setStartDelay(180).start()
        binding.description.alpha = 0f
        binding.description.animate().alpha(1f).setDuration(460).setStartDelay(280).start()
    }

    private fun pageFromArguments(): OnboardingPage? {
        val args = arguments ?: return null
        return OnboardingPage(
            eyebrow = args.getString("eyebrow").orEmpty(),
            title = args.getString("title").orEmpty(),
            description = args.getString("description").orEmpty(),
            icon = args.getInt("icon", R.drawable.ic_note),
            accent = args.getInt("accent", 0),
            highlights = args.getString("highlights").orEmpty()
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(page: OnboardingPage): OnboardingFragment {
            val fragment = OnboardingFragment()
            fragment.arguments = Bundle().apply {
                putString("eyebrow", page.eyebrow)
                putString("title", page.title)
                putString("description", page.description)
                putInt("icon", page.icon)
                putInt("accent", page.accent)
                putString("highlights", page.highlights)
            }
            return fragment
        }
    }
}
