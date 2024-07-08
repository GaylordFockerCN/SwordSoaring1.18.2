package net.p1nero.ss.epicfight.skill;

import net.minecraft.resources.ResourceLocation;
import net.p1nero.ss.SwordSoaring;
import yesman.epicfight.api.forgeevent.SkillRegistryEvent;
import yesman.epicfight.skill.DodgeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

public class ModSkills {

    public static Skill SWORD_SOARING;

    public static void BuildSkills(SkillRegistryEvent event){
        SWORD_SOARING = event.registerSkill(
                new SwordSoaringSkill(DodgeSkill.createBuilder(new ResourceLocation(SwordSoaring.MOD_ID, "sword_soaring"))
                        .setCategory(SkillCategories.PASSIVE).setResource(Skill.Resource.NONE)), true);
    }

}
