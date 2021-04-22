package com.fenbi.megrez.app.exercisescope.flowlayout

/**
 * Created by lei.jialin on 2021/4/22
 */
data class ChoiceTag<T>(
    val name: String,
    val data: T,
    var isSelect: Boolean = false
)