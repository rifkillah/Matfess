package com.matfess.app.ui.academic

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.databinding.FragmentDocumentsBinding
import com.matfess.app.ui.common.BaseFragment

class DocumentsFragment : BaseFragment() {

    private var _binding: FragmentDocumentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DocumentAdapter
    private var filter = 0 // 0 semua, 1 modul, 2 sertifikat
    private var allDocs: List<com.matfess.app.data.Document> = emptyList()

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        showDocumentEditor(null, uri) { reload() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DocumentAdapter(
            onOpen = { document -> openDocument(document) },
            onMore = { document -> showOptions(document) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.segmentedType.setItems(listOf("Semua", "Modul", "Sertifikat"), 0)
        binding.segmentedType.setOnSegmentChanged {
            filter = it
            reload()
        }
        binding.btnAdd.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) reload()
    }

    private fun reload() {
        val b = _binding ?: return
        allDocs = repo.db.getDocuments()
        val filtered = when (filter) {
            1 -> allDocs.filter { it.type == "module" }
            2 -> allDocs.filter { it.type == "certificate" }
            else -> allDocs
        }
        adapter.submitList(filtered)
        b.recycler.isVisible = filtered.isNotEmpty()
        b.tvEmpty.isVisible = filtered.isEmpty()
        b.tvEmpty.text = when {
            allDocs.isEmpty() -> "Brankas masih kosong. Simpan modul kuliah atau sertifikat dengan tombol +."
            filter == 1 -> "Belum ada modul tersimpan."
            filter == 2 -> "Belum ada sertifikat tersimpan."
            else -> "Tidak ada berkas pada filter ini."
        }
        b.tvCount.text = "${allDocs.size} berkas · ${allDocs.count { it.type == "certificate" }} sertifikat"
    }

    private fun openDocument(document: com.matfess.app.data.Document) {
        val uri = runCatching { Uri.parse(document.uri) }.getOrNull()
        if (uri == null) {
            toast("Berkas tidak ditemukan.")
            return
        }
        val mime = requireContext().contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, "Buka ${document.title}"))
        } catch (_: ActivityNotFoundException) {
            toast("Belum ada aplikasi yang bisa membuka berkas ini.")
        } catch (_: SecurityException) {
            toast("Akses berkas sudah tidak valid. Pilih ulang berkasnya.")
        }
    }

    private fun showOptions(document: com.matfess.app.data.Document) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(document.title)
            .setItems(arrayOf("Buka berkas", "Ubah detail", "Hapus dari brankas")) { _, which ->
                when (which) {
                    0 -> openDocument(document)
                    1 -> showDocumentEditor(document) { reload() }
                    else -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hapus berkas?")
                        .setMessage("\"${document.title}\" akan dihapus dari brankas. Berkas asli di perangkat tetap aman.")
                        .setNegativeButton("Batal", null)
                        .setPositiveButton("Hapus") { _, _ ->
                            repo.db.deleteDocument(document.id)
                            reload()
                            toast("Berkas dihapus dari brankas.")
                        }
                        .show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        _binding?.recycler?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
