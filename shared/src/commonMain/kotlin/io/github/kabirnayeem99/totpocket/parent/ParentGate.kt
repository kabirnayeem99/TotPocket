package io.github.kabirnayeem99.totpocket.parent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/** "a + b = ?" with three answers in random order, exactly one of them right. */
@Immutable
data class GateQuestion(val a: Int, val b: Int, val answers: List<Int>) {
    val correct: Int get() = a + b
}

/**
 * A sum a toddler can't read or solve but a grown-up answers at a glance. Wrong answers just get
 * a fresh question — there's no lock-out, so a grown-up is never stuck.
 */
object ParentGateQuestions {
    fun next(random: Random): GateQuestion {
        val a = random.nextInt(3, 10)
        val b = random.nextInt(3, 10)
        val correct = a + b
        val wrong = mutableSetOf<Int>()
        while (wrong.size < 2) {
            val candidate = correct + random.nextInt(-4, 5)
            if (candidate != correct && candidate > 0) wrong += candidate
        }
        return GateQuestion(a, b, (wrong + correct).shuffled(random))
    }
}

sealed interface ParentGateAction {
    data class AnswerPicked(val answer: Int) : ParentGateAction
}

sealed interface ParentGateEffect {
    data object Unlocked : ParentGateEffect
}

class ParentGateViewModel(private val random: Random) : ViewModel() {

    private val _question = MutableStateFlow(ParentGateQuestions.next(random))
    val question: StateFlow<GateQuestion> = _question.asStateFlow()

    private val _effects = Channel<ParentGateEffect>(Channel.BUFFERED)
    val effects: Flow<ParentGateEffect> = _effects.receiveAsFlow()

    fun onAction(action: ParentGateAction) {
        when (action) {
            is ParentGateAction.AnswerPicked -> {
                if (action.answer == _question.value.correct) {
                    _effects.trySend(ParentGateEffect.Unlocked)
                } else {
                    _question.update { ParentGateQuestions.next(random) }
                }
            }
        }
    }
}
