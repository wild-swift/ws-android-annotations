package name.wildswift.testapp

import android.app.Activity
import android.os.Bundle
import name.wildswift.lib.androidkotlinannotations.ActivityField
import name.wildswift.lib.androidkotlinannotations.RandomFunction
import name.wildswift.lib.androidkotlinannotations.RandomFunctionParameter

@ActivityField(
        name = "id", type = Long::class, nullable = true
)
@RandomFunction(
        parameters = [
            RandomFunctionParameter(name = "context", type = Int::class, nullable = false)
        ],
        dictionary = [
            "name.wildswift.testapp.random1",
            "name.wildswift.testapp.random2",
            "name.wildswift.testapp.random3",
            "name.wildswift.testapp.random4",
            "name.wildswift.testapp.random5",
            "name.wildswift.testapp.random6",
            "name.wildswift.testapp.random7",
            "name.wildswift.testapp.random8",
            "name.wildswift.testapp.random9",
            "name.wildswift.testapp.random10"
        ]
)
internal class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
