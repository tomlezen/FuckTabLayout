# FuckTabLayout
FuckTabLayout是直接修改的TabLayout源码，将java代码改为了kotlin代码，没有改动其原有的api，并在基础上增加滑动文字颜色渐变以及角标设置等功能

<img src="https://github.com/tomlezen/FuckTabLayout/blob/master/screenshot/ezgif.com-video-to-gif.gif?raw=true" alt="arc" style="max-width:100%;">

## Gradle

```
implementation 'com.github.tomlezen:FuckTabLayout:1.0.0'
```
## 使用

api使用与TabLayout一致，如果需要设置指示器宽度与文字宽度一致，设置`tabIndicatorFullWidth`属性为`false`即可

```
添加小红点(默认color为red, radius为2dp):
FuckTabLayout.addDotBadge(index, color, radius)
添加数字角标(默认color为red, textColor为white，textSize为11sp):
FuckTabLayout.addNumberBadge(index, number, color, textColor, textSize)
自定义角标
FuckTabLayout.addBadge(index, object: Badge(color){
            override fun getMeasureWidth(): Int = 20

            override fun getMeasureHeight(): Int = 20

            override fun draw(cvs: Canvas, drawnRectF: RectF) {
                // 绘制
            }
})
移除角标
FuckTabLayout.removeBadge(index)
获取角标
FuckTabLayout.getBadge(index)
```
具体细节可参考[MainActivity](https://github.com/tomlezen/FuckTabLayout/blob/master/app/src/main/java/com/tlz/fucktablayout/example/MainActivity.kt)使用
