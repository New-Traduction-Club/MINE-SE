# MINE

>### Disclaimer and Notice
>
>MINE does not include, package, or distribute any game assets, artwork, scripts, or audio from proprietary visual novels or mods, including Doki Doki Literature Club! and Monika After Story. Users are required to provide their own legally obtained game files and mods archives.
>
>This project is an independent, open-source Android wrapper and runtime environment developed by Traduction Club!. It is not affiliated with, endorsed by, or associated with Team Salvato, the Monika After Story development team, or PyTom and the official Ren'Py team.

## Overview

MINE (MASL Is Not an Emulator) is a native Android launcher, runtime environment, and visual novel manager built upon a customized fork of Ren'Py's Android Packaging Tool (RAPT). Designed to improve the performance and compatibility limitations of full-system emulation or translation layers, MINE executes Ren'Py visual novels natively on modern Android devices.

Originally conceived as an Android wrapper for MAS mod, the project has evolved into a general-purpose Ren'Py environment. It delivers a full-featured desktop interface on mobile devices, pairing multi-engine support with storage management, per-game customization, and integrated tools for decompilation.

## Multi-Runtime

MINE embeds independent runtimes covering both Python 2 and Python 3 environments. Supported versions include Ren'Py 6.99, 7.4.11, 7.8.4, 8.0.3, 8.3.7, 8.4.1, and 8.5.3.

Each game can be explicitly assigned to any installed runtime version. Additionally, MINE incorporates an automated version detection engine that analyzes archive structures, version, and engine bytecode upon installation. This system determines whether the target game requires a Python 2 or Python 3 runtime and automatically selects the optimal engine version.

## Game and Mod Management

The launcher provides an automated installation workflow supporting standard Ren'Py game distributions as well as DDLC mods. When importing DDLC mods, the installer pairs the modification archive with the user-provided official base game archive, verifying file integrity through SHA-256 checksum validation before staging the files.

## Integrated Tools

The toolset includes a Ren'Py archive extractor implemented directly in Kotlin for unpacking RPA archives. An integrated RPYC decompiler. Through an embedded Chaquopy Python environment, MINE provides an Python script runner capable of executing Python scripts on-device, with real-time logging and standard input interaction. 
Tools can operate across both internal application directories and external system storage via Android's SAF.

## Virtual Desktop Integration

MINE organizes its interface around a virtual desktop capable of running activities within windowed and maximized modes. The workspace includes custom wallpaper management, taskbar window controls, and a built-in File Explorer equipped with breadcrumb navigation, search functionality, and file manipulation capabilities.

## Acknowledgments and Credits

[Ren'Py](https://www.renpy.org/) visual novel engine and the RAPT framework, created by PyTom and contributors.

Unrpa archive extraction logic, originally designed by [Lattyware](https://github.com/lattyware/unrpa).

Unrpyc script decompiler, by [CensoredUsername](https://github.com/CensoredUsername/unrpyc).

Chaquopy embedded Python SDK for Android, developed by [Chaquo](https://github.com/chaquo/chaquopy).

Team Salvato and the Monika After Story team, whose creative work inspired the origins of this launcher.

## License

This project is licensed under the terms of the MIT License. Individual third-party components, including Ren'Py runtimes, unrpyc, and Chaquopy dependencies, remain subject to their respective upstream licenses.
