package com.matfess.app.ui.academic

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AcademicPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        1 -> ScheduleFragment()
        2 -> TasksFragment()
        3 -> DocumentsFragment()
        else -> NotesFragment()
    }
}
