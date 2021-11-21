class Vars:
    def __init__(self, scriptid, gw):
        self.scriptid = scriptid
        self.gw = gw
        self.ui = self.ui()

    def toJStringArray(self, arr):
        jarr = self.gw.new_array(self.gw.jvm.java.lang.String, len(arr))
        for i in range(len(arr)):
            jarr[i] = arr[i]
        return jarr

    def pbotapi(self):
        return self.gw.jvm.haven.purus.pbot.PBotAPI

    # current ui
    def ui(self):
        return self.pbotapi().ui()

    def uis(self):
        return self.pbotapi().uis()

    def pbotutils(self):
        return self.gw.jvm.haven.purus.pbot.PBotUtils

    def pbotcharacterapi(self):
        return self.gw.jvm.haven.purus.pbot.PBotCharacterAPI

    def pbotdiscord(self):
        return self.gw.jvm.haven.purus.pbot.PBotDiscord

    def pbotgobapi(self):
        return self.gw.jvm.haven.purus.pbot.PBotGobAPI

    def pbotwindowapi(self):
        return self.gw.jvm.haven.purus.pbot.PBotWindowAPI
