package org.renpy.android

import android.content.Context
import java.util.Locale

object WelcomeContent {

    fun getMarkdown(context: Context, isGps: Boolean): String {
        val prefs = context.getSharedPreferences(BaseActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val prefLang = prefs.getString("language", null)?.lowercase(Locale.ROOT)
        val systemLang = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language.lowercase(Locale.ROOT)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language.lowercase(Locale.ROOT)
        }

        val isSpanish = prefLang?.contains("español") == true ||
                prefLang?.contains("spanish") == true ||
                prefLang == "es" ||
                (prefLang == null && systemLang.startsWith("es"))

        val isPortuguese = prefLang?.contains("português") == true ||
                prefLang?.contains("portugues") == true ||
                prefLang?.contains("portuguese") == true ||
                prefLang == "pt" ||
                (prefLang == null && systemLang.startsWith("pt"))

        return when {
            isSpanish -> if (isGps) ES_GPS_TRUE else ES_GPS_FALSE
            isPortuguese -> if (isGps) PT_GPS_TRUE else PT_GPS_FALSE
            else -> if (isGps) EN_GPS_TRUE else EN_GPS_FALSE
        }
    }

    private const val ES_GPS_TRUE = """¡Hola!

Bienvenido/a a _MINE_, primero debo de agradecerte por el hecho de que hayas comprado la aplicación, esto nos ayudará un montón para poder sustentar los gastos que implica toda la infraestructura de _Traduction Club!_.

Ya que por si no lo sabes, _MINE_ cuenta con una versión totalmente gratuita en nuestro [repositorio de GitHub](https://github.com/New-Traduction-Club/MINE-SE/releases), la cual es identica a esta versión que tienes, no hay ninguna función exclusiva en la app de paga. No nos gusta privatizar funciones a cambio de dinero, pero tampoco estamos cerrados a un apoyo económico, dicho de otra manera, aceptar donaciones. Es por esta misma razón que hemos decidido lanzar la app en la Play Store con un costo fijo.

Dicho esto, sigamos con la información general.

---

_MINE_ es una app que te permite ejecutar juegos hechos con [Ren'Py](https://www.renpy.org/) de manera directa y nativa en tu dispositivo (celular, tableta, Chromebook...), esto es posible gracias a que usamos las librerías de _Ren'Py_ para Android, pero con ligeras modificaciones hechas por nosotros para asegurar la compatibilidad y rendimiento.

Como algunos sabrán, _MINE_ nació de la idea de usar [MASL](https://github.com/New-Traduction-Club/MonikaAfterStory-Android-port) para ejecutar cualquier juego _Ren'Py_, es por ello que en el asistente de instalación de juegos agregamos soporte para instalar mods de DDLC de una manera más facil. Aunque hay que recalcar que algunos mods se tendrán que instalar manualmente ya que requieren pasos muy específicos (borrar scripts.rpa, por ejemplo).

Si tienes dudas en como instalar algún juego/mod o inclusive si tienes un error al intentar abrirlo, puedes pedir ayuda en [nuestro servidor de Discord](https://discord.gg/usKs37Sh5S).

Sin más preámbulos, ¡disfruta de _MINE_!

> Esta pantalla solo aparecerá una vez, aunque puedes consultarla desde Ajustes."""

    private const val ES_GPS_FALSE = """¡Hola!

Bienvenido/a a _MINE_, te comento que estás usando la verisón gratuita de la app, contamos con una versión de paga en [Play Store](https://play.google.com/store/apps/details?id=com.z.tdclub.mine.se). Aunque si estás usando la versión gratuita es porque no quieras gastar o directamente la economía no lo permite... Aunque no te preocupes, no hay ninguna función exclusiva en la versión de paga, solo sirve por si quieres apoyarnos y/o hacer una donación.

Si te gusta _MINE_, por favor considera algún día apoyarnos comprando la app en la Play Store, ¡sería de mucha ayuda!

Dicho esto, sigamos con la información general.

---

_MINE_ es una app que te permite ejecutar juegos hechos con [Ren'Py](https://www.renpy.org/) de manera directa y nativa en tu dispositivo (celular, tableta, Chromebook...), esto es posible gracias a que usamos las librerías de _Ren'Py_ para Android, pero con ligeras modificaciones hechas por nosotros para asegurar la compatibilidad y rendimiento.

Como algunos sabrán, _MINE_ nació de la idea de usar [MASL](https://github.com/New-Traduction-Club/MonikaAfterStory-Android-port) para ejecutar cualquier juego _Ren'Py_, es por ello que en el asistente de instalación de juegos agregamos soporte para instalar mods de DDLC de una manera más facil. Aunque hay que recalcar que algunos mods se tendrán que instalar manualmente ya que requieren pasos muy específicos (borrar scripts.rpa, por ejemplo).

Si tienes dudas en como instalar algún juego/mod o inclusive si tienes un error al intentar abrirlo, puedes pedir ayuda en [nuestro servidor de Discord](https://discord.gg/usKs37Sh5S).

Sin más preámbulos, ¡disfruta de _MINE_!

> Esta pantalla solo aparecerá una vez, aunque puedes consultarla desde Ajustes."""

    private const val EN_GPS_TRUE = """Hello!

Welcome to _MINE_! First of all, I want to thank you for purchasing the app. Your support goes a long way in helping us cover the infrastructure costs for _Traduction Club!_

In case you didn't know, a completely free version of _MINE_ is available on our [GitHub repository](https://github.com/New-Traduction-Club/MINE-SE/releases). It's exactly the same as the version you have right now—there are no exclusive features locked behind the paid version. We don't believe in paywalling features, but we are always open to financial support (in other words, donations). That is exactly why we decided to release the app on the Play Store for a fixed price.

With that out of the way, let's get into the main details.

---

_MINE_ is an app that allows you to run [Ren'Py](https://www.renpy.org/) games directly and natively on your device (phone, tablet, Chromebook, etc.). This is possible because we use the official Android _Ren'Py_ libraries, along with a few slight modifications of our own to guarantee optimal compatibility and performance.

As some of you may know, _MINE_ was born from the idea of using [MASL](https://github.com/New-Traduction-Club/MonikaAfterStory-Android-port) to run any _Ren'Py_ game. Because of this, we've added built-in support to our game installation wizard to make installing DDLC mods a breeze. However, please keep in mind that some mods will still need to be installed manually if they require very specific steps (such as deleting scripts.rpa, for example).

If you have any questions about how to install a game or mod, or if you run into an error when trying to open one, feel free to ask for help on [our Discord server](https://discord.gg/usKs37Sh5S).

Without further ado, enjoy _MINE_!

> This screen will only appear once, though you can always view it again from the Settings menu."""

    private const val EN_GPS_FALSE = """Hello!

Welcome to _MINE_! Just to let you know, you are currently using the free version of the app. We also have a paid version available on the [Play Store](https://play.google.com/store/apps/details?id=com.z.tdclub.mine.se). Whether you prefer not to spend money or finances are just a bit tight right now, we totally get it. But don't worry—there are no exclusive features locked behind the paid version. It simply exists as a way for users to support us and make a donation if they wish.

If you enjoy using _MINE_, please consider supporting us someday by purchasing the app on the Play Store. It would be a huge help!

With that out of the way, let's get into the main details.

---

_MINE_ is an app that allows you to run [Ren'Py](https://www.renpy.org/) games directly and natively on your device (phone, tablet, Chromebook, etc.). This is possible because we use the official Android _Ren'Py_ libraries, along with a few slight modifications of our own to guarantee optimal compatibility and performance.

As some of you may know, _MINE_ was born from the idea of using [MASL](https://github.com/New-Traduction-Club/MonikaAfterStory-Android-port) to run any _Ren'Py_ game. Because of this, we've added built-in support to our game installation wizard to make installing DDLC mods a breeze. However, please keep in mind that some mods will still need to be installed manually if they require very specific steps (such as deleting scripts.rpa, for example).

If you have any questions about how to install a game or mod, or if you run into an error when trying to open one, feel free to ask for help on [our Discord server](https://discord.gg/usKs37Sh5S).

Without further ado, enjoy _MINE_!

> This screen will only appear once, though you can always view it again from the Settings menu."""

    private const val PT_GPS_TRUE = """Olá!

Seja bem-vindo(a) ao _MINE_! Primeiramente, eu gostaria de agradecer muito por você ter comprado o aplicativo. O seu apoio nos ajuda demais a cobrir os custos para manter toda a infraestrutura do _Traduction Club!_.

Caso você não saiba, o _MINE_ conta com uma versão totalmente gratuita disponível em nosso [repositório no GitHub](https://github.com/New-Traduction-Club/MINE-SE/releases). Ela é exatamente igual à versão que você tem agora — não existe nenhuma funcionalidade exclusiva no aplicativo pago. Nós não gostamos de bloquear recursos em troca de dinheiro, mas também estamos sempre abertos a receber apoio financeiro (ou seja, doações). É exatamente por esse motivo que decidimos lançar o aplicativo na Play Store com um valor fixo.

Com isso em mente, vamos às informações gerais.

---

O _MINE_ é um aplicativo que permite rodar jogos feitos com [Ren'Py](https://www.renpy.org/) de forma direta e nativa no seu dispositivo (celular, tablet, Chromebook, etc.). Isso só é possível porque utilizamos as bibliotecas oficiais do _Ren'Py_ para Android, mas com algumas modificações próprias para garantir a melhor compatibilidade e desempenho.

Como alguns já devem saber, o _MINE_ nasceu da ideia de usar o [MASL](https://github.com/New-Traduction-Club/MonikaAfterStory-Android-port) para rodar qualquer jogo em _Ren'Py_. Por conta disso, adicionamos um suporte integrado ao nosso assistente de instalação de jogos para tornar a instalação de mods de DDLC muito mais fácil. No entanto, vale ressaltar que alguns mods ainda precisarão ser instalados manualmente, pois exigem etapas muito específicas (como apagar o arquivo scripts.rpa, por exemplo).

Se você tiver alguma dúvida sobre como instalar um jogo ou mod, ou até mesmo se esbarrar em algum erro ao tentar abrir um deles, fique à vontade para pedir ajuda em [nosso servidor do Discord](https://discord.gg/usKs37Sh5S).

Sem mais delongas, aproveite o _MINE_!

> Esta tela aparecerá apenas uma vez, mas você sempre pode acessá-la novamente através das Configurações."""

    private const val PT_GPS_FALSE = """Olá!

Seja bem-vindo(a) ao _MINE_! Só para avisar, no momento você está usando a versão gratuita do aplicativo. Nós também temos uma versão paga disponível na [Play Store](https://play.google.com/store/apps/details?id=com.z.tdclub.mine.se). Se você optou pela versão gratuita porque prefere não gastar no momento ou porque a grana está meio curta, nós entendemos perfeitamente. Mas não se preocupe, não existe nenhuma funcionalidade exclusiva na versão paga. Ela serve apenas como uma forma de nos apoiar e/ou fazer uma doação.

Se você gosta do _MINE_, por favor, considere nos apoiar um dia comprando o aplicativo na Play Store. Isso nos ajudaria demais!

Com isso em mente, vamos às informações gerais.

---

O _MINE_ é um aplicativo que permite rodar jogos feitos com [Ren'Py](https://www.renpy.org/) de forma direta e nativa no seu dispositivo (celular, tablet, Chromebook, etc.). Isso só é possível porque utilizamos as bibliotecas oficiais do _Ren'Py_ para Android, mas com algumas modificações próprias para garantir a melhor compatibilidade e desempenho.

Como alguns já devem saber, o _MINE_ nasceu da ideia de usar o [MASL](https://github.com/New-Traduction-Club/MonikaAfterStory-Android-port) para rodar qualquer jogo em _Ren'Py_. Por conta disso, adicionamos um suporte integrado ao nosso assistente de instalação de jogos para tornar a instalação de mods de DDLC muito mais fácil. No entanto, vale ressaltar que alguns mods ainda precisarão ser instalados manualmente, pois exigem etapas muito específicas (como apagar o arquivo scripts.rpa, por exemplo).

Se você tiver alguma dúvida sobre como instalar um jogo ou mod, ou até mesmo se esbarrar em algum erro ao tentar abrir um deles, fique à vontade para pedir ajuda em [nosso servidor do Discord](https://discord.gg/usKs37Sh5S).

Sem mais delongas, aproveite o _MINE_!

> Esta tela aparecerá apenas uma vez, mas você sempre pode acessá-la novamente através das Configurações."""
}
