package com.cryptocompare.baselineprofile

import android.widget.EditText
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Снимает baseline profile: какие классы и методы нужны на холодном старте и
 * на главных путях. С ним ART компилирует их заранее при установке, а не
 * интерпретирует первые запуски — это холодный старт и первые прокрутки
 * каталога без рывков.
 *
 * Путь — то, что делает почти каждый: старт, каталог с прокруткой, экран пары
 * с графиком, портфель. Запуск: `baselineprofile/README.md`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = BaselineProfileConstants.TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()

            skipOnboarding()
            browseCatalog()
            openPair()
            openPortfolio()
        }
}

/** На чистой установке первым идёт онбординг; повторный прогон его уже не увидит. */
private fun MacrobenchmarkScope.skipOnboarding() {
    device
        .wait(
            Until.findObject(By.text(BaselineProfileConstants.ONBOARDING_SKIP)),
            BaselineProfileConstants.SCREEN_TIMEOUT_MS,
        )?.click()
    device.wait(
        Until.hasObject(By.text(BaselineProfileConstants.TAB_PAIRS)),
        BaselineProfileConstants.SCREEN_TIMEOUT_MS,
    )
}

private fun MacrobenchmarkScope.browseCatalog() {
    flingLargestScrollable(Direction.DOWN)
    flingLargestScrollable(Direction.UP)
}

/**
 * Пара открывается через поиск, а не первой строкой каталога: наверху по
 * алфавиту стоят малоликвидные пары, у которых бирж может и не найтись, и
 * профиль снимал бы экран ошибки вместо графика.
 */
private fun MacrobenchmarkScope.openPair() {
    device.findObject(By.clazz(EditText::class.java))?.text = BaselineProfileConstants.PAIR_QUERY
    device
        .wait(
            Until.findObject(By.text(BaselineProfileConstants.PAIR_NAME)),
            BaselineProfileConstants.NETWORK_TIMEOUT_MS,
        )?.click() ?: return

    // график грузится из сети: ждём масштабы, они появляются вместе с ним
    device.wait(
        Until.hasObject(By.text(BaselineProfileConstants.TIMEFRAME_H1)),
        BaselineProfileConstants.NETWORK_TIMEOUT_MS,
    )
    flingLargestScrollable(Direction.DOWN)
    device.pressBack()
    device.wait(
        Until.hasObject(By.text(BaselineProfileConstants.TAB_PAIRS)),
        BaselineProfileConstants.SCREEN_TIMEOUT_MS,
    )
}

private fun MacrobenchmarkScope.openPortfolio() {
    device.findObject(By.text(BaselineProfileConstants.TAB_PORTFOLIO))?.click()
    device.waitForIdle()
    device.findObject(By.text(BaselineProfileConstants.TAB_PAIRS))?.click()
    device.waitForIdle()
}

/**
 * Прокрутка главного списка экрана. Список ищется заново перед каждым жестом
 * и при устаревании ищется ещё раз: пока подгружается страница каталога,
 * Compose пересобирает его, и найденный секунду назад элемент уже не живой.
 */
private fun MacrobenchmarkScope.flingLargestScrollable(direction: Direction) {
    repeat(BaselineProfileConstants.GESTURE_ATTEMPTS) {
        val list = catalogList() ?: return
        try {
            list.setGestureMargin(device.displayWidth / BaselineProfileConstants.GESTURE_MARGIN_FRACTION)
            list.fling(direction)
            device.waitForIdle()
            return
        } catch (stale: StaleObjectException) {
            device.waitForIdle()
        }
    }
}

/**
 * Главный список экрана — самый высокий прокручиваемый элемент: ряд чипов над
 * каталогом тоже прокручивается, но вбок и узкой полосой.
 */
private fun MacrobenchmarkScope.catalogList(): UiObject2? {
    device.wait(Until.hasObject(By.scrollable(true)), BaselineProfileConstants.NETWORK_TIMEOUT_MS)
    return device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }
}
