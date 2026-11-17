package ar.edu.uade.salastecnicas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.uade.salastecnicas.ui.AppNavigation
import ar.edu.uade.salastecnicas.ui.theme.SalasTecnicasTheme
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModel
import ar.edu.uade.salastecnicas.viewmodel.RobotViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repositorio = (application as SalasApp).repositorio
        setContent {
            SalasTecnicasTheme {
                val vm: RobotViewModel = viewModel(
                    factory = RobotViewModelFactory(repositorio)
                )
                AppNavigation(vm)
            }
        }
    }
}
