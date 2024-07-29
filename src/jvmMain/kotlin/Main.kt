import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rest.interpreter.external.rest.RestClient
import com.rest.interpreter.model.PersistentTab
import com.rest.interpreter.persistence.Persistence
import com.rest.interpreter.ui.FontEndFactory.createHeader
import com.rest.interpreter.ui.FontEndFactory.dynamicHeaderCreate
import com.rest.interpreter.ui.FontEndFactory.verbComponent
import com.rest.interpreter.ui.TabContentFactory
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.window.rememberWindowState

@Preview
@Composable
fun App() {
    MaterialTheme {
        val client = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
        MainScreen(client)
    }
}
//stash


@Composable
fun MainScreen(client: HttpClient) {
    var selectedTab by remember { mutableStateOf(0) }
    var isCollectionExpanded by remember { mutableStateOf(false) }
    var isEnvironmentExpanded by remember { mutableStateOf(false) }

    Persistence.createDatabase()


    val tabs by remember {
        mutableStateOf(Persistence.getTabs().map { persistentTab ->
            TabContentFactory.TabItem("Request" + persistentTab.id, persistentTab,
                screen = { tabContent(client, persistentTab) })
        }.toMutableStateList())
    }


    fun addTab(){
        val newTab = updateTheDatabase(
            PersistentTab(
                0, "", "www.example.com", "GET", "", "", listOf()
            )
        )
        println("Created new request ${newTab}")
        val content =
            TabContentFactory.TabItem("Request" + newTab?.id.toString(), newTab!!,
                screen = {
                    tabContent(client, newTab)
                })
        tabs.add(content)
        selectedTab = tabs.indexOf(content)
    }


    Surface {
        Row(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight()
                    .background(Color.White)

            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Button(onClick = {isCollectionExpanded=!isCollectionExpanded; isEnvironmentExpanded=false},
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White))
                    {Text("Collections", fontSize = 12.sp)}

                    Spacer(modifier = Modifier.height(100.dp))

                    Button(onClick = {isEnvironmentExpanded=!isEnvironmentExpanded; isCollectionExpanded=false},
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White))
                    {Text("Environments", fontSize = 12.sp)}
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.Black)
                )
            }
            if(isCollectionExpanded)
                collection_section()
            if(isEnvironmentExpanded)
                environment_section()
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(Color.White)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    contentColor = Color.LightGray,
                    backgroundColor = Color.Blue,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.Green
                        )
                    }
                ) {

                    tabs.forEachIndexed { index, tab ->
                        Box(modifier=Modifier.width(20.dp)){
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(tab.title) }
                            )
                            IconButton(onClick = {
                                tabs.remove(tab)
                                when{
                                    selectedTab==0 ->  addTab()
                                    selectedTab>=tabs.size -> selectedTab--
                                }
                                Persistence.delete(tab.persistentTab)
                            },modifier = Modifier.align(Alignment.CenterEnd)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }

                    }
                    Button(
                        onClick = {
                            addTab()
                        },
                    ) {
                        Text("+")
                    }
                }
                TabContentFactory.TabContent(tabs[selectedTab].screen)
                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.Blue)
                ){
                    Button(onClick = {

                    },
                        modifier = Modifier.align(Alignment.Center),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)

                    ){
                        Text("Send")
                    }
                }
            }
        }
    }

//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("DBPostman") },
//                backgroundColor = Color.Blue,
//                contentColor = Color.White
//            )
//
//            TabRow(
//                selectedTabIndex = selectedTab,
//                backgroundColor = Color.Blue,
//                contentColor = Color.Green,
//                modifier = Modifier.width(644.dp)
//            ) {
//                tabs.forEachIndexed { index, tab ->
//                    Tab(
//                        selected = selectedTab == index,
//                        onClick = { selectedTab = index },
//                        text = { Text(tab.title) }
//                    )
//                    IconButton(onClick = {
//                        tabs.remove(tab)
//                        Persistence.delete(tab.persistentTab)
//                    }) {
//                        Icon(Icons.Default.Delete, contentDescription = "Delete")
//                    }
//                }
//
//                Button(
//                    onClick = {
//                        val newTab = updateTheDatabase(
//                            PersistentTab(
//                                0, "", "www.example.com", "GET", "", "", listOf()
//                            )
//                        )
//                        println("Created new request ${newTab}")
//                        val content = TabContentFactory.TabItem("Request" + newTab?.id.toString(), newTab!!,
//                            screen = {
//                                tabContent(client, newTab)
//                            })
//                        tabs.add(content)
//                        selectedTab = tabs.indexOf(content)
//
//                    },
//                ) {
//                    Text("+")
//                }
//            }
//        },
//        content = {
//            Column(modifier = Modifier.fillMaxSize()) {
//                TabContentFactory.TabContent(tabs[selectedTab].screen)
//            }
//        }
//    )
}

@Composable
fun tabContent(client: HttpClient, tab: PersistentTab) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val tabState = mutableStateOf(Persistence.getTabById(tab.id))

        verbComponent(tabState)

        urlComponent(tabState)

        tabState.value.headers?.forEachIndexed { index, header ->
            createHeader(header, tabState.value.headers?.toMutableStateList()!!, index, tabState)
        }

        if (tabState.value.verb == "POST" || tabState.value.verb == "PUT") {
            bodyComponent(tabState)
        }

        dynamicHeaderCreate(client, tabState)

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                runBlocking {
                    val res = RestClient.sendRequest(client, tabState)
                    tabState.value = tabState.value.copy(content = res)
                    updateTheDatabase(tabState.value)
                }

            }
        ) {
            Text("Send Request")
        }

        responseComponent(tabState)
    }
}

@Composable
fun bodyComponent(tab: MutableState<PersistentTab>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = tab.value.requestBody.toString(),
            onValueChange = {
                tab.value = tab.value.copy(requestBody = it)
                updateTheDatabase(tab.value)
            },
            label = { Text("Request Body") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun updateTheDatabase(persistentTab: PersistentTab): PersistentTab? {
    println("Request to save $persistentTab")
    return Persistence.save(persistentTab)
}

@Composable
fun urlComponent(tab: MutableState<PersistentTab>) {
    OutlinedTextField(
        value = tab.value.url,
        onValueChange = {
            tab.value = tab.value.copy(url = it)
            updateTheDatabase(tab.value)
        },
        label = { Text("URL") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun responseComponent(tab: MutableState<PersistentTab>) {
    println(tab.value.content)
    Text(
        text = tab.value.content.orEmpty(),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.body1
    )
}


@Composable
fun collection_section(){
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color.LightGray)
            .padding(16.dp)
    ) {

        Text("Collection Item 1")
        Text("Collection Item 2")
        Text("Collection Item 3")


    }
}

@Composable
fun environment_section(){
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color.LightGray)
            .padding(16.dp)
    ) {

        Text("Environment Item 1")
        Text("Environment Item 2")
        Text("Environment Item 3")


    }
}






fun main() = application {
    Window(
        title = "dbPostman",
        state = rememberWindowState(
            width = 1080.dp,
            height = 700.dp
        ),
        onCloseRequest = ::exitApplication
    ) {
        App()

    }
}



