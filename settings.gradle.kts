rootProject.name = "Sky4th-Server"

// 定义所有模块
include("SkyCore")
include("CreditEconomy")
include("Infrastructure")
include("ParallelDungeon")
include("SkySkills")
include("SkyMissions")
include("BetterMC")
include("BetterVillage")
include("EquipmentAffix")

project(":SkyCore").projectDir = file("SkyCore")
project(":CreditEconomy").projectDir = file("CreditEconomy")
project(":Infrastructure").projectDir = file("Infrastructure")
project(":ParallelDungeon").projectDir = file("ParallelDungeon")
project(":SkySkills").projectDir = file("SkySkills")
project(":SkyMissions").projectDir = file("SkyMissions")
project(":BetterMC").projectDir = file("BetterMC")
project(":BetterVillage").projectDir = file("BetterVillage")
project(":EquipmentAffix").projectDir = file("EquipmentAffix")
