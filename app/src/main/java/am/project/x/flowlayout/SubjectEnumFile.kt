package com.fenbi.megrez.app.exercisescope.flowlayout

/**
 * Created by lei.jialin on 2021/4/22
 */

enum class DictationSequence(val title: String, val storeKey: String) {
    Order("顺序", "order"),
    ReverseOrder("逆序", "reverse_order")
}

enum class DeconSubjectChinese(val title: String, val storeKey: String) {
    ProfessionalTeacher("专业老师版", "professional_teacher"),
    CuteChild("小猿童声版", "cute_child")
}

enum class DeaconSubjectEnglish(val title: String, val storeKey: String) {
    English("朗读英文", "english"),
    Mandarin("朗读中文", "mandarin"),
    EnglishAndMandarin("朗读中+英", "english_And_mandarin")
}

enum class PauseGapTime(val title: String, val storeKey: String) {
    Seconds4("4s", "4s"),
    Seconds6("6s", "6s"),
    Seconds8("8s", "8s")
}

