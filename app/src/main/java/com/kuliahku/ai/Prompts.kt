package com.kuliahku.ai

object Prompts {

    fun summary(text: String): String = """
        Anda adalah asisten belajar untuk mahasiswa Indonesia.
        Ringkas materi berikut menjadi poin-poin utama dalam Bahasa Indonesia.
        Aturan:
        - Maksimal 8 poin.
        - Gunakan kalimat singkat dan jelas.
        - JANGAN menambahkan fakta di luar materi.
        - Jika materi tidak jelas, tulis apa adanya.

        Materi:
        $text
    """.trimIndent()

    fun quiz(text: String, count: Int): String = """
        Buat $count soal pilihan ganda (A-D) berdasarkan materi berikut.
        Output HARUS JSON valid dengan schema:
        {
          "questions": [
            {
              "question": "teks soal",
              "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
              "correctIndex": 0,
              "explanation": "penjelasan singkat",
              "topic": "topik spesifik dari soal"
            }
          ]
        }
        Aturan:
        - Hanya berdasarkan materi.
        - correctIndex adalah 0-3.
        - Bahasa Indonesia.
        - Jangan tambahkan teks di luar JSON.

        Materi:
        $text
    """.trimIndent()

    fun flashcards(text: String, count: Int): String = """
        Buat $count flashcard berdasarkan materi berikut.
        Output HARUS JSON valid:
        {
          "cards": [
            { "front": "pertanyaan/istilah", "back": "jawaban/definisi singkat" }
          ]
        }
        Aturan:
        - Hanya berdasarkan materi.
        - Bahasa Indonesia.
        - Front singkat (maks 1 kalimat), Back singkat (maks 2 kalimat).

        Materi:
        $text
    """.trimIndent()

    fun chat(text: String, question: String): String = """
        Anda adalah asisten belajar. Jawab pertanyaan mahasiswa HANYA berdasarkan materi berikut.
        Jika jawaban tidak ditemukan di materi, katakan: "Tidak ditemukan di materi."
        Jangan mengarang.

        Materi:
        $text

        Pertanyaan: $question
    """.trimIndent()

    fun references(text: String): String = """
        Berdasarkan materi berikut, ekstrak 3-5 kata kunci pencarian (search keywords) dalam Bahasa Inggris dan Bahasa Indonesia yang paling relevan untuk mencari referensi jurnal akademik di OpenAlex.
        Output HARUS JSON valid:
        {
          "keywords": ["keyword 1", "keyword 2", "keyword 3"]
        }
        Aturan:
        - Fokus pada topik inti materi.
        - Gunakan istilah teknis/akademik.
        - Jangan tambahkan teks di luar JSON.

        Materi:
        $text
    """.trimIndent()

    fun taskPrioritization(tasksJson: String): String = """
        Anda adalah asisten produktivitas. Analisis daftar tugas berikut dan berikan saran urutan pengerjaan yang paling efisien berdasarkan deadline, kompleksitas (dari deskripsi), dan prioritas.
        Berikan saran dalam 3-5 poin singkat dalam Bahasa Indonesia.
        Jelaskan alasan singkat mengapa urutan tersebut disarankan.

        Daftar Tugas (JSON):
        $tasksJson
    """.trimIndent()

    fun financeInsight(summaryJson: String): String = """
        Anda adalah asisten perencana keuangan mahasiswa. Analisis ringkasan transaksi bulanan berikut.
        Berikan 3 insight utama:
        1. Evaluasi pengeluaran terbesar.
        2. Saran penghematan yang realistis untuk mahasiswa.
        3. Prediksi kondisi keuangan akhir bulan jika pola berlanjut.
        
        Gunakan Bahasa Indonesia yang santai tapi profesional. Maksimal 150 kata.

        Ringkasan Keuangan (JSON):
        $summaryJson
    """.trimIndent()
}
