package com.fenbi.megrez.app.exercisescope.flowlayout

/**
 * Created by lei.jialin on 2021/4/22
 */
class ChoiceGroup<T>(
    val groupTitle: String?,
    val choices: List<ChoiceTag<T>> = emptyList(),
    var lastSelect: ChoiceTag<T>? = null
) {

}