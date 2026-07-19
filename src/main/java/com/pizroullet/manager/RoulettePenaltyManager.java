package com.pizroullet.manager;

import com.pizroullet.util.RewardType;
import com.pizroullet.util.RouletteTier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Random;

public class RoulettePenaltyManager {

    private static final Random RANDOM = new Random();
    private static org.bukkit.Location previousLocation = null;

    public static void executeReward(RewardType type) {
        if (type == null) return;

        // 공통 등급 사운드 연출 실행
        playTierSound(type.getTier());

        switch (type) {
            // ================= 1단계 벌칙 =================
            case MONSTER_BUNDLE_1:
                executeMonsterBundle(1);
                break;
            case DIAMOND_INCREASE:
                executeDiamondIncrease();
                break;
            case DEBUFF:
                executeDebuff();
                break;
            case RESIZE_SMALL:
                executeResizeSmall();
                break;
            case DROP_ALL:
                executeDropAll();
                break;
            case COBWEB_PRISON:
                executeCobwebPrison();
                break;

            // ================= 2단계 벌칙 =================
            case MONSTER_BUNDLE_2:
                executeMonsterBundle(2);
                break;
            case ZOMBIE_RAID:
                executeZombieRaid();
                break;
            case SILVERFISH_RAID:
                executeSilverfishRaid();
                break;
            case SAND_PRISON:
                executeSandPrison();
                break;
            case TNT_SPAWN:
                executeTntSpawn();
                break;
            case RANDOM_TELEPORT:
                executeRandomTeleport();
                break;
            case LAVA_SPAWN:
                executeLavaFlood();
                break;


            // ================= 3단계 벌칙 =================
            case MONSTER_BUNDLE_3:
                executeMonsterBundle(3);
                break;
            case JUMP_MAP_1:
                executeJumpMap(1);
                break;
            case SKYDIVING_1:
                executeSkydiving(1);
                break;

            // ================= 4단계 벌칙 =================
            case MONSTER_BUNDLE_4:
                executeMonsterBundle(4);
                break;
            case JUMP_MAP_2:
                executeJumpMap(2);
                break;
            case SKYDIVING_2:
                executeSkydiving(2);
                break;
            case INSTANT_DIE:
                executePlayerKill();
                break;
        }
    }

    /**
     * 등급에 따른 사운드를 모든 온라인 플레이어에게 재생하는 재사용 메서드
     */
    private static void playTierSound(RouletteTier tier) {
        Sound targetSound = tier.getSound();

        // 최종 결정된 사운드를 final 상수로 복사 (람다식 내부 사용용)
        final Sound playedSound = targetSound;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), playedSound, 1.0f, 1.0f);
        }
    }

    // ----------------- 벌칙 구현 메서드 서브셋 -----------------

    // [1단계] 다버리기
    private static void executeDropAll() {
        // 전역 온라인 유저가 아니라, 현재 등록된 게임 참가자 1명만 조준
        Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        Location loc = player.getLocation();
        PlayerInventory inv = player.getInventory();

        org.bukkit.NamespacedKey pickaxeKey = new org.bukkit.NamespacedKey(com.pizroullet.PizRoullet.getInstance(), "roulette_pickaxe");

        // getContents() 배열을 인덱스로 직접 돌면서 하나씩 체크하고 제거해야 안전합니다.
        org.bukkit.inventory.ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;

            // 특수 곡괭이 태그 검사 -> 있으면 버리기 패스
            if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(pickaxeKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                continue;
            }

            // 특수 곡괭이가 아닌 일반 아이템은 플레이어 위치에 바닥 드롭하고 엔티티 객체 확보
            org.bukkit.entity.Item droppedItem = player.getWorld().dropItemNaturally(loc, item);

            if (droppedItem != null) {
                // 40틱(2초) 동안 플레이어가 이 아이템을 다시 주워 먹지 못하도록 지연 시간 설정
                droppedItem.setPickupDelay(40);
            }

            // 인벤토리의 해당 칸만 정확히 비우기
            inv.setItem(i, null);
        }
    }

    // 몬스터 보따리 (1~4단계 공통 확장용)
    // 각 단계별 스폰 가능한 몬스터 타입 풀 정의
    private static final org.bukkit.entity.EntityType[] MONSTERS_TIER_1 = {
            org.bukkit.entity.EntityType.SPIDER,
            org.bukkit.entity.EntityType.CAVE_SPIDER,
            org.bukkit.entity.EntityType.WOLF, // 타겟 설정을 통해 화난 늑대 구현
            org.bukkit.entity.EntityType.ZOMBIE,
            org.bukkit.entity.EntityType.SKELETON,
            org.bukkit.entity.EntityType.CREEPER,
            org.bukkit.entity.EntityType.SLIME,
            org.bukkit.entity.EntityType.SILVERFISH,
            org.bukkit.entity.EntityType.BLAZE,
            org.bukkit.entity.EntityType.MAGMA_CUBE,
            org.bukkit.entity.EntityType.WITCH,
            org.bukkit.entity.EntityType.STRAY,
            org.bukkit.entity.EntityType.HUSK,
            org.bukkit.entity.EntityType.PILLAGER,
            org.bukkit.entity.EntityType.ENDERMITE,
            org.bukkit.entity.EntityType.BOGGED
    };

    private static final org.bukkit.entity.EntityType[] MONSTERS_TIER_2 = {
            org.bukkit.entity.EntityType.WITHER_SKELETON,
            org.bukkit.entity.EntityType.VINDICATOR,
            org.bukkit.entity.EntityType.VEX,
            org.bukkit.entity.EntityType.ENDERMAN, // 타겟 설정을 통해 화난 엔더맨 구현
            org.bukkit.entity.EntityType.PIGLIN,
            org.bukkit.entity.EntityType.SKELETON_HORSE, // 기병 처리는 아래에서 별도 분기
            org.bukkit.entity.EntityType.BREEZE,
            org.bukkit.entity.EntityType.ZOMBIE_HORSE,   // 기병 처리는 아래에서 별도 분기
            org.bukkit.entity.EntityType.CHICKEN,      // 치킨 조키용
            org.bukkit.entity.EntityType.CREEPER // 충전된 크리퍼 처리를 위해 일반 크리퍼 추가
    };

    private static final org.bukkit.entity.EntityType[] MONSTERS_TIER_3 = {
            org.bukkit.entity.EntityType.HOGLIN,
            org.bukkit.entity.EntityType.RAVAGER,
            org.bukkit.entity.EntityType.PIGLIN_BRUTE

    };

    /**
     * [벌칙 공통] 몬스터 보따리 (단계별 스폰 메커니즘)
     * 1단계: 1단계 몬스터 1종류를 3마리 소환
     * 2단계: 2단계 몬스터 1마리 소환
     * 3단계: 2단계 몬스터 3마리 소환 (매번 랜덤 종류)
     * 4단계: 3단계 몬스터 1마리 소환
     */
    private static void executeMonsterBundle(int bundleTier) {
        Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 1. 플레이어 주변 블록 청소
        clearSurroundingBlocks(player);

        switch (bundleTier) {
            case 1:
                // 1단계 배열에서 미리 '한 가지 종류'를 선정한 뒤, 그 종류로만 3마리 소환
                if (MONSTERS_TIER_1 != null && MONSTERS_TIER_1.length > 0) {
                    org.bukkit.entity.EntityType selectedType = MONSTERS_TIER_1[RANDOM.nextInt(MONSTERS_TIER_1.length)];
                    for (int i = 0; i < 3; i++) {
                        spawnSpecificMonster(player, selectedType, 1);
                    }
                }
                break;

            case 2:
                // 2단계 몬스터 1마리 소환
                spawnRandomMonster(player, 2);
                break;

            case 3:
                if (MONSTERS_TIER_2 != null && MONSTERS_TIER_2.length > 0) {
                    org.bukkit.entity.EntityType selectedType = MONSTERS_TIER_2[RANDOM.nextInt(MONSTERS_TIER_2.length)];
                    for (int i = 0; i < 3; i++) {
                        spawnSpecificMonster(player, selectedType, 2);
                    }
                }
                break;

            case 4:
                // 3단계 몬스터 1마리 소환
                spawnRandomMonster(player, 3);
                break;
        }
    }

    /**
     * 기존 메서드: 등급(Tier)을 받아 내부에서 무작위 종류를 결정하고 소환합니다.
     */
    private static void spawnRandomMonster(Player player, int monsterTier) {
        org.bukkit.entity.EntityType type = null;
        if (monsterTier == 1 && MONSTERS_TIER_1 != null && MONSTERS_TIER_1.length > 0) {
            type = MONSTERS_TIER_1[RANDOM.nextInt(MONSTERS_TIER_1.length)];
        } else if (monsterTier == 2 && MONSTERS_TIER_2 != null && MONSTERS_TIER_2.length > 0) {
            type = MONSTERS_TIER_2[RANDOM.nextInt(MONSTERS_TIER_2.length)];
        } else if (monsterTier == 3 && MONSTERS_TIER_3 != null && MONSTERS_TIER_3.length > 0) {
            type = MONSTERS_TIER_3[RANDOM.nextInt(MONSTERS_TIER_3.length)];
        }

        if (type != null) {
            spawnSpecificMonster(player, type, monsterTier);
        }
    }

    private static void spawnSpecificMonster(Player player, org.bukkit.entity.EntityType type, int monsterTier) {
        if (type == null) return;

        Location pLoc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        Location spawnLoc;

        // 티어 3 몬스터는 플레이어의 정확한 현재 위치에 소환
        if (monsterTier == 3) {
            spawnLoc = new Location(world, pLoc.getX(), pLoc.getY(), pLoc.getZ(), pLoc.getYaw(), pLoc.getPitch());
        } else {
            // 1. 플레이어 기준 반경 2.0 ~ 3.5 블록 사이의 무작위 X, Z 오프셋 연산
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = 2.0 + (RANDOM.nextDouble() * 1.5);
            int spawnX = (int) Math.round(pLoc.getX() + (Math.cos(angle) * distance));
            int spawnZ = (int) Math.round(pLoc.getZ() + (Math.sin(angle) * distance));

            // 2. [끼임 방지 핵심 강화] 안전 고도 정밀 탐색
            int spawnY = pLoc.getBlockY();
            boolean found = false;

            for (int y = pLoc.getBlockY() - 3; y <= pLoc.getBlockY() + 3; y++) {
                org.bukkit.block.Block feet = world.getBlockAt(spawnX, y, spawnZ);
                org.bukkit.block.Block air1 = world.getBlockAt(spawnX, y + 1, spawnZ);
                org.bukkit.block.Block air2 = world.getBlockAt(spawnX, y + 2, spawnZ);

                if (feet.getType().isSolid() && air1.getType().isAir() && air2.getType().isAir()) {
                    spawnY = y + 1;
                    found = true;
                    break;
                }
            }

            if (!found) {
                spawnY = pLoc.getBlockY();
            }

            spawnLoc = new Location(world, spawnX + 0.5, spawnY, spawnZ + 0.5);
        }

        // 3. 특수 콤보 몹 처리 (탑승 메커니즘 조립)
        if (type == org.bukkit.entity.EntityType.SKELETON_HORSE) {
            org.bukkit.entity.SkeletonHorse horse = world.spawn(spawnLoc, org.bukkit.entity.SkeletonHorse.class);
            org.bukkit.entity.Skeleton rider = world.spawn(spawnLoc, org.bukkit.entity.Skeleton.class);
            horse.addPassenger(rider);
            rider.setTarget(player);
            return;
        }
        if (type == org.bukkit.entity.EntityType.ZOMBIE_HORSE) {
            org.bukkit.entity.ZombieHorse horse = world.spawn(spawnLoc, org.bukkit.entity.ZombieHorse.class);
            org.bukkit.entity.Zombie rider = world.spawn(spawnLoc, org.bukkit.entity.Zombie.class);
            horse.addPassenger(rider);
            rider.setTarget(player);
            return;
        }
        if (type == org.bukkit.entity.EntityType.CHICKEN) {
            org.bukkit.entity.Chicken chicken = world.spawn(spawnLoc, org.bukkit.entity.Chicken.class);
            org.bukkit.entity.Zombie rider = world.spawn(spawnLoc, org.bukkit.entity.Zombie.class);
            rider.setBaby();
            chicken.addPassenger(rider);
            rider.setTarget(player);
            return;
        }

        // 4. 일반 몬스터 안전 스폰 및 타겟 AI 강제 바인딩
        org.bukkit.entity.Entity entity = world.spawn(spawnLoc, type.getEntityClass());

        if (entity instanceof org.bukkit.entity.Creature) {
            org.bukkit.entity.Creature creature = (org.bukkit.entity.Creature) entity;
            if (creature instanceof org.bukkit.entity.Wolf) {
                ((org.bukkit.entity.Wolf) creature).setAngry(true);
            }
            creature.setTarget(player);

            // [추가] 파괴수 혹은 호글린일 경우 돌진 시 블록 파괴 메커니즘 바인딩
            if (type == org.bukkit.entity.EntityType.RAVAGER || type == org.bukkit.entity.EntityType.HOGLIN) {
                // 5틱(0.25초)마다 반복 수행하는 타이머 스케줄러 시작
                org.bukkit.Bukkit.getScheduler().runTaskTimer(com.pizroullet.PizRoullet.getInstance(), new java.util.function.Consumer<org.bukkit.scheduler.BukkitTask>() {
                    @Override
                    public void accept(org.bukkit.scheduler.BukkitTask task) {
                        // 몹이 죽었거나 서버에서 사라지면 스케줄러 안전하게 종료
                        if (creature.isDead() || !creature.isValid()) {
                            task.cancel();
                            return;
                        }
                        // 주변 블록 파괴 로직 실행
                        destroyBlocksAround(creature);
                    }
                }, 0L, 5L);
            }
        }

        // 2티어 크리퍼인 경우 번개 맞은 차징 크리퍼 상태로 변환
        if (entity instanceof org.bukkit.entity.Creeper && monsterTier == 2) {
            ((org.bukkit.entity.Creeper) entity).setPowered(true);
        }
    }


    /**
     * 지정한 엔티티 주변의 블록을 파괴하고 시각 효과를 줍니다.
     */
    private static void destroyBlocksAround(org.bukkit.entity.LivingEntity entity) {
        if (entity == null || entity.isDead()) return;

        org.bukkit.Location loc = entity.getLocation();
        org.bukkit.World world = entity.getWorld();

        // 엔티티 몸통 주변 (가로 3x3, 세로 2칸) 범위 탐색
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    org.bukkit.block.Block targetBlock = world.getBlockAt(
                            loc.getBlockX() + x,
                            loc.getBlockY() + y,
                            loc.getBlockZ() + z
                    );

                    // 공기나 베드락처럼 부수면 안 되는 블록 필터링
                    if (targetBlock.getType().isAir() || targetBlock.getType() == org.bukkit.Material.BEDROCK) {
                        continue;
                    }

                    // 블록 파괴 (아이템 드롭 없이 파편 이펙트와 소리만 재생)
                    world.spawnParticle(org.bukkit.Particle.BLOCK, targetBlock.getLocation().add(0.5, 0.5, 0.5), 10, targetBlock.getBlockData());
                    world.playSound(targetBlock.getLocation(), org.bukkit.Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                    targetBlock.setType(org.bukkit.Material.AIR);
                }
            }
        }
    }

    /**
     * [벌칙] 다이아 목표 증가 (DIAMOND_TARGET_INCREASE)
     * 참여자가 깨야 하는 다이아몬드 목표 수량을 5개 강제로 늘려서 난이도를 올립니다.
     */
    private static void executeDiamondIncrease() {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // RouletteManager에 새로 추가한 목표치 증가 메서드 호출
        RouletteManager.increaseTargetDiamonds(5);
    }

    // [1단계] 디버프
    private static void executeDebuff() {
        // TODO: 부정적인 포션 효과 부여
    }

    // 작아지기 벌칙의 기존 예약 타이머를 추적하기 위한 세션 변수
    private static org.bukkit.scheduler.BukkitTask resizeTask = null;

    /**
     * [벌칙 1단계] 작아지기 (RESIZE_SMALL)
     * 참여 플레이어의 크기를 0.5배, 점프력을 0.5배(0.21)로 1분간 축소합니다.
     */
    private static void executeResizeSmall() {
        Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 크기 속성과 점프력 속성 인스턴스 가져오기
        org.bukkit.attribute.AttributeInstance scaleAttribute =
                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        org.bukkit.attribute.AttributeInstance jumpAttribute =
                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_JUMP_STRENGTH);

        if (scaleAttribute == null || jumpAttribute == null) return;

        // 이미 기존에 돌아가고 있던 타이머가 있다면 취소하여 시간 초기화(갱신)
        if (resizeTask != null) {
            resizeTask.cancel();
            resizeTask = null;
        }

        // 플레이어 크기 0.5 축소 & 점프력을 절반(기본값 0.42 -> 0.21)으로 축소
        scaleAttribute.setBaseValue(0.5);
        jumpAttribute.setBaseValue(0.35);

        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.5f);

        // 1분(1200틱) 뒤 복구하는 타이머 스케줄러 등록
        resizeTask = Bukkit.getScheduler().runTaskLater(com.pizroullet.PizRoullet.getInstance(), () -> {
            if (player.isOnline()) {
                org.bukkit.attribute.AttributeInstance currentScale =
                        player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                org.bukkit.attribute.AttributeInstance currentJump =
                        player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_JUMP_STRENGTH);

                if (currentScale != null && currentJump != null) {
                    currentScale.setBaseValue(1.0); // 크기 원복
                    currentJump.setBaseValue(0.42); // 점프력 원복 (마인크래프트 기본값)

                    player.sendMessage("§6[다이아 룰렛] §a신체 크기와 점프력이 원래대로 돌아왔습니다.");
                }
            }
            resizeTask = null;
        }, 1200L);
    }

    // [1단계] 거미줄 감옥
    /**
     * [벌칙] 거미줄 감옥 (COBWEB_PRISON)
     * 주변 공간을 청소한 후, 참여자 중심으로 5x5x2(높이) 범위에 거미줄을 설치하여 가둡니다.
     */
    private static void executeCobwebPrison() {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 1. 주변 7x7x7 공간 청소 및 발광석 배치
        clearSurroundingBlocks(player);

        org.bukkit.Location center = player.getLocation();
        org.bukkit.World world = center.getWorld();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // 2. 5x5x2 범위 거미줄 설치 (X, Z는 좌우 2칸씩 총 5칸 / Y는 발을 딛는 눈높이부터 위로 2칸)
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);

                    // 기반암(Bedrock)은 건드리지 않도록 안전 처리
                    if (block.getType() == org.bukkit.Material.BEDROCK) {
                        continue;
                    }

                    // 거미줄 블록으로 변경
                    block.setType(org.bukkit.Material.COBWEB, false);
                }
            }
        }
    }

    /**
     * [벌칙] 용암 설치 (LAVA_FLOOD)
     * 주변 공간을 청소한 후, 참여자의 한 칸 위를 기준으로 7x7 범위에 용암을 설치합니다.
     */
    private static void executeLavaFlood() {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 1. 주변 5x5x7 공간 청소 및 발광석 배치 (V2 버전 호출)
        clearSurroundingBlocksV2(player);

        org.bukkit.Location center = player.getLocation();
        org.bukkit.World world = center.getWorld();

        int centerX = center.getBlockX();
        // 플레이어의 위치에서 한 칸 위를 기준 Y축으로 설정
        int floorY = center.getBlockY() + 2;
        int centerZ = center.getBlockZ();

        // 2. 한 칸 위 7x7 범위에 용암 설치 (반경 -3 ~ +3 총 7칸)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                org.bukkit.block.Block block = world.getBlockAt(centerX + x, floorY, centerZ + z);

                // 기반암(Bedrock)은 파괴되거나 바뀌지 않도록 안전 필터링
                if (block.getType() == org.bukkit.Material.BEDROCK) {
                    continue;
                }

                // 용암 블록으로 변경 및 물리 업데이트 활성화 (false -> true)
                block.setType(org.bukkit.Material.LAVA, true);
            }
        }
    }

    // [2단계] 좀비 습격
    /**
     * [벌칙 2단계] 좀비 습격 (ZOMBIE_RAID)
     * 플레이어 주변 7x7x7 공간을 청소하고 발광석을 설치한 뒤,
     * 참여자를 타겟팅하는 좀비 10마리를 안전하게 스폰합니다.
     */
    private static void executeZombieRaid() {
        Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 1. 플레이어 주변 7x7x7 공간 청소 및 발광석 배치
        clearSurroundingBlocks(player);

        // 2. 좀비 10마리 순차적 안전 스폰
        org.bukkit.World world = player.getWorld();
        for (int i = 0; i < 10; i++) {
            Location pLoc = player.getLocation();

            // 플레이어 기준 반경 2.0 ~ 3.5 블록 사이의 무작위 X, Z 오프셋 연산 (끼임 방지)
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = 2.0 + (RANDOM.nextDouble() * 1.5);
            int spawnX = (int) Math.round(pLoc.getX() + (Math.cos(angle) * distance));
            int spawnZ = (int) Math.round(pLoc.getZ() + (Math.sin(angle) * distance));

            // 최고 높이 바닥 추적 (지형 매립 방지)
            int spawnY = world.getHighestBlockYAt(spawnX, spawnZ);
            if (Math.abs(spawnY - pLoc.getBlockY()) > 3) {
                spawnY = pLoc.getBlockY();
            } else {
                spawnY += 1;
            }

            Location spawnLoc = new Location(world, spawnX + 0.5, spawnY, spawnZ + 0.5);

            // 좀비 엔티티 생성
            org.bukkit.entity.Zombie zombie = world.spawn(spawnLoc, org.bukkit.entity.Zombie.class);

            // 생성된 좀비가 즉시 참여 플레이어를 공격하도록 AI 설정
            if (zombie != null) {
                zombie.setTarget(player);
            }
        }

        // 벌칙 발동 알림 및 사운드 효과
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.0f);
    }

    /**
     * [벌칙 2단계] 좀벌레 습격 (SILVERFISH_RAID)
     * 지하인 경우 주변 15x15x15 안의 모든 돌과 심층암을 감염된 블록으로 바꿉니다.
     * 지상(주변에 돌이 없음)인 경우 좀벌레 20마리를 즉시 소환하여 습격합니다.
     */
    private static void executeSilverfishRaid() {
        Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        org.bukkit.Location center = player.getLocation();
        org.bukkit.World world = center.getWorld();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // 1. 소리 효과 및 플레이어 정중앙 위치에 좀벌레 5마리 즉시 소환
        player.playSound(center, org.bukkit.Sound.ENTITY_SILVERFISH_HURT, 1.0f, 0.8f);

        for (int i = 0; i < 5; i++) {
            // 오차 범위를 완전히 없애고 플레이어의 현재 X, Z 좌표 정중앙에 맞춤
            // 발밑 블록에 끼지 않도록 Y축만 살짝(+0.1) 올린 위치
            org.bukkit.Location spawnLoc = new org.bukkit.Location(
                    world,
                    center.getX(),
                    center.getY() + 0.1,
                    center.getZ(),
                    center.getYaw(),
                    center.getPitch()
            );

            org.bukkit.entity.Silverfish silverfish = world.spawn(spawnLoc, org.bukkit.entity.Silverfish.class);
            if (silverfish != null) {
                silverfish.setTarget(player);
            }
        }

        // 2. 15x15x15 범위 탐색하여 돌/심층암을 감염된 블록으로 즉시 변환
        for (int x = -7; x <= 7; x++) {
            for (int y = -7; y <= 7; y++) {
                for (int z = -7; z <= 7; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                    org.bukkit.Material type = block.getType();

                    if (type == org.bukkit.Material.STONE) {
                        block.setType(org.bukkit.Material.INFESTED_STONE, false);
                    } else if (type == org.bukkit.Material.DEEPSLATE) {
                        block.setType(org.bukkit.Material.INFESTED_DEEPSLATE, false);
                    }
                }
            }
        }
    }

    // [2단계] 모래 감옥
    /**
     * [벌칙] 모래 감옥 (SAND_PRISON)
     * 주변 공간을 청소한 후, 참여자 중심으로 5x5x5 범위에 흘러내리는 모래 엔티티를 생성하여 매립합니다.
     */
    private static void executeSandPrison() {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 1. 주변 7x7x7 공간 청소 및 발광석 배치
        clearSurroundingBlocks(player);

        org.bukkit.Location center = player.getLocation();
        org.bukkit.World world = center.getWorld();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // 2. 5x5x5 범위에 떨어지는 모래 엔티티(FallingBlock) 스폰
        // Y축은 발밑(0)부터 위로 4까지 설정하여 허공에서 쏟아지도록 유도
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    // 스폰될 정확한 위치 계산 (블록의 중앙 좌표인 +0.5 세팅)
                    org.bukkit.Location spawnLoc = new org.bukkit.Location(
                            world,
                            centerX + x + 0.5,
                            centerY + y + 0.5,
                            centerZ + z + 0.5
                    );

                    // 해당 좌표의 원래 블록이 기반암인 경우 패스
                    if (world.getBlockAt(spawnLoc).getType() == org.bukkit.Material.BEDROCK) {
                        continue;
                    }

                    // 일반 블록 배치 대신 중력 모래 엔티티 생성
                    world.spawnFallingBlock(spawnLoc, org.bukkit.Bukkit.createBlockData(org.bukkit.Material.SAND));
                }
            }
        }
    }

    /**
     * [벌칙] TNT 소환 (TNT_SPAWN)
     * 주변 공간을 청소한 후, 참여자 주변에 점화된 TNT 4개를 4틱 간격으로 총 3번 소환합니다.
     */
    private static void executeTntSpawn() {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        // 1. 최초 1회 주변 7x7x7 공간 청소 및 발광석 배치
        clearSurroundingBlocks(player);

        org.bukkit.plugin.Plugin plugin = com.pizroullet.PizRoullet.getInstance();
        org.bukkit.Location pLoc = player.getLocation();
        org.bukkit.World world = player.getWorld();

        // 2. 1차 소환 (즉시 실행)
        for (int i = 0; i < 4; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = 1.5 + (RANDOM.nextDouble() * 1.5);
            int spawnX = (int) Math.round(pLoc.getX() + (Math.cos(angle) * distance));
            int spawnZ = (int) Math.round(pLoc.getZ() + (Math.sin(angle) * distance));
            int spawnY = world.getHighestBlockYAt(spawnX, spawnZ);
            spawnY = (Math.abs(spawnY - pLoc.getBlockY()) > 3) ? pLoc.getBlockY() : spawnY + 1;

            org.bukkit.Location spawnLoc = new org.bukkit.Location(world, spawnX + 0.5, spawnY + 0.5, spawnZ + 0.5);
            org.bukkit.entity.TNTPrimed tnt = world.spawn(spawnLoc, org.bukkit.entity.TNTPrimed.class);
            if (tnt != null) {
                tnt.setFuseTicks(30); // 1.5초 후 폭발
            }
        }

        // 3. 2차 소환 (4틱 뒤 실행)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 4; i++) {
                double angle = RANDOM.nextDouble() * 2 * Math.PI;
                double distance = 1.5 + (RANDOM.nextDouble() * 1.5);
                int spawnX = (int) Math.round(pLoc.getX() + (Math.cos(angle) * distance));
                int spawnZ = (int) Math.round(pLoc.getZ() + (Math.sin(angle) * distance));
                int spawnY = world.getHighestBlockYAt(spawnX, spawnZ);
                spawnY = (Math.abs(spawnY - pLoc.getBlockY()) > 3) ? pLoc.getBlockY() : spawnY + 1;

                org.bukkit.Location spawnLoc = new org.bukkit.Location(world, spawnX + 0.5, spawnY + 0.5, spawnZ + 0.5);
                org.bukkit.entity.TNTPrimed tnt = world.spawn(spawnLoc, org.bukkit.entity.TNTPrimed.class);
                if (tnt != null) {
                    tnt.setFuseTicks(30);
                }
            }
        }, 4L);

        // 4. 3차 소환 (8틱 뒤 실행 - 2차 소환으로부터 다시 4틱 뒤)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 4; i++) {
                double angle = RANDOM.nextDouble() * 2 * Math.PI;
                double distance = 1.5 + (RANDOM.nextDouble() * 1.5);
                int spawnX = (int) Math.round(pLoc.getX() + (Math.cos(angle) * distance));
                int spawnZ = (int) Math.round(pLoc.getZ() + (Math.sin(angle) * distance));
                int spawnY = world.getHighestBlockYAt(spawnX, spawnZ);
                spawnY = (Math.abs(spawnY - pLoc.getBlockY()) > 3) ? pLoc.getBlockY() : spawnY + 1;

                org.bukkit.Location spawnLoc = new org.bukkit.Location(world, spawnX + 0.5, spawnY + 0.5, spawnZ + 0.5);
                org.bukkit.entity.TNTPrimed tnt = world.spawn(spawnLoc, org.bukkit.entity.TNTPrimed.class);
                if (tnt != null) {
                    tnt.setFuseTicks(30);
                }
            }
        }, 8L);
    }

    /**
     * 플레이어 주변 안전 고도 바닥에 점화된 TNT를 지정된 수량만큼 스폰합니다.
     */
    private static void spawnIgnitedTntBatch(org.bukkit.entity.Player player, int count) {
        if (player == null || !player.isOnline()) return;

        org.bukkit.Location pLoc = player.getLocation();
        org.bukkit.World world = player.getWorld();

        for (int i = 0; i < count; i++) {
            // 플레이어 중심 반경 1.5 ~ 3.0 블록 사이 무작위 좌표 연산
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = 1.5 + (RANDOM.nextDouble() * 1.5);
            int spawnX = (int) Math.round(pLoc.getX() + (Math.cos(angle) * distance));
            int spawnZ = (int) Math.round(pLoc.getZ() + (Math.sin(angle) * distance));

            // 지형 끼임 방지 바닥 Y축 추적
            int spawnY = world.getHighestBlockYAt(spawnX, spawnZ);
            if (Math.abs(spawnY - pLoc.getBlockY()) > 3) {
                spawnY = pLoc.getBlockY();
            } else {
                spawnY += 1;
            }

            org.bukkit.Location spawnLoc = new org.bukkit.Location(world, spawnX + 0.5, spawnY + 0.5, spawnZ + 0.5);

            // 점화된 TNT 엔티티 스폰 (기본 퓨즈 타임 적용됨)
            world.spawn(spawnLoc, org.bukkit.entity.TNTPrimed.class);
        }
    }

    /**
     * [벌칙] 랜덤 텔레포트 (RANDOM_TELEPORT)
     * 현재 위치 기준 500x500 범위 내의 안전한 무작위 좌표로 플레이어를 이동시킵니다.
     */
    private static void executeRandomTeleport() {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        org.bukkit.Location currentLoc = player.getLocation();
        org.bukkit.World world = currentLoc.getWorld();

        // 현재 위치 기준 반경 -500 ~ +500 범위 계산 (총 1000x1000 범위)
        int randomX = currentLoc.getBlockX() + (RANDOM.nextInt(1001) - 500);
        int randomZ = currentLoc.getBlockZ() + (RANDOM.nextInt(1001) - 500);

        // 해당 X, Z 좌표에서 가장 높은 블록의 Y 고도를 안전하게 가져옴 (질식 방지)
        int safeY = world.getHighestBlockYAt(randomX, randomZ);

        // 블록 정중앙에 안전하게 착지하도록 소수점 0.5 보정 및 시선(Yaw/Pitch) 유지
        org.bukkit.Location targetLoc = new org.bukkit.Location(
                world,
                randomX + 0.5,
                safeY + 1.0,
                randomZ + 0.5,
                currentLoc.getYaw(),
                currentLoc.getPitch()
        );

        // 메인 스레드에서 안전하게 텔레포트 수행
        player.teleport(targetLoc);
    }

    // 점프맵 (3~4단계 공통 확장용)
    private static void executeJumpMap(int tier) {
        // TODO: 점프맵 스테이지 생성 또는 이동
    }

    /**
     * [벌칙] 스카이다이빙 (SKYDIVING)
     * 플레이어 위치에서 가장 높은 블록을 기준으로 지정된 고도만큼 위로 순간이동시킵니다.
     * @param tier 1인 경우 +25블록, 2인 경우 +200블록
     */
    private static void executeSkydiving(int tier) {
        org.bukkit.entity.Player player = RouletteManager.getParticipant();
        if (player == null || !player.isOnline()) return;

        org.bukkit.Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();

        // 현재 X, Z 좌표에서 가장 높은 블록의 Y축 가져오기
        int highestY = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());

        // 티어별 추가 고도 설정
        int addHeight = (tier == 1) ? 200 : 1000;

        // 최종 목적지 고도 계산 및 좌표 생성 (소수점 중앙 정렬)
        double targetY = highestY + addHeight;
        org.bukkit.Location teleportLoc = new org.bukkit.Location(
                world,
                loc.getBlockX() + 0.5,
                targetY,
                loc.getBlockZ() + 0.5,
                loc.getYaw(),
                loc.getPitch()
        );

        // 순간이동 실행
        player.teleport(teleportLoc);
    }

    private static void clearSurroundingBlocks(Player player) {
        if (player == null || !player.isOnline()) return;

        Location center = player.getLocation();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY(); // 플레이어 발 위치 (y = 0)
        int centerZ = center.getBlockZ();
        org.bukkit.World world = center.getWorld();

        // 1. 7x7x7 범위 공기로 청소 (Y축을 플레이어 위치인 0부터 6까지 총 7칸)
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y <= 6; y++) {
                for (int z = -3; z <= 3; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                    org.bukkit.Material type = block.getType();

                    if (type == org.bukkit.Material.AIR ||
                            type == org.bukkit.Material.BEDROCK ||
                            type == org.bukkit.Material.SAND ||
                            type == org.bukkit.Material.COBWEB ||
                            type == org.bukkit.Material.LAVA) {
                        continue;
                    }

                    block.setType(org.bukkit.Material.AIR);
                }
            }
        }

        // 지운 공간의 맨 위쪽 레이어(y = 6)에 발광석 배치
        int glowstoneY = centerY + 6;

        // 발광석 설치
        world.getBlockAt(centerX + 3, glowstoneY, centerZ).setType(org.bukkit.Material.GLOWSTONE, false); // 동쪽 끝
        world.getBlockAt(centerX - 3, glowstoneY, centerZ).setType(org.bukkit.Material.GLOWSTONE, false); // 서쪽 끝
        world.getBlockAt(centerX, glowstoneY, centerZ + 3).setType(org.bukkit.Material.GLOWSTONE, false); // 남쪽 끝
        world.getBlockAt(centerX, glowstoneY, centerZ - 3).setType(org.bukkit.Material.GLOWSTONE, false); // 북쪽 끝
    }

    /**
     * 지정된 플레이어 중심 5x5x7(높이) 범위의 블록을 공기로 변경하여 제거합니다.
     * 추가로 사방 끝 벽면에 시야 확보용 발광석(GLOWSTONE)을 안전하게 매립합니다.
     */
    private static void clearSurroundingBlocksV2(Player player) {
        if (player == null || !player.isOnline()) return;

        Location center = player.getLocation();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY(); // 플레이어 발 위치 (y = 0)
        int centerZ = center.getBlockZ();
        org.bukkit.World world = center.getWorld();

        // 1. 5x5x7 범위 공기로 청소 (X: 5칸, Z: 5칸, Y: 0부터 6까지 총 7칸)
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 6; y++) {
                for (int z = -2; z <= 2; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                    org.bukkit.Material type = block.getType();

                    if (type == org.bukkit.Material.AIR ||
                            type == org.bukkit.Material.BEDROCK ||
                            type == org.bukkit.Material.SAND ||
                            type == org.bukkit.Material.COBWEB ||
                            type == org.bukkit.Material.LAVA) {
                        continue;
                    }

                    block.setType(org.bukkit.Material.AIR);
                }
            }
        }

        // 지운 공간의 맨 위쪽 레이어(y = 6)에 발광석 배치
        int glowstoneY = centerY + 6;

        // 5x5 범위의 끝 벽면(반경 2블록 위치)에 발광석 설치
        world.getBlockAt(centerX + 2, glowstoneY, centerZ).setType(org.bukkit.Material.GLOWSTONE, false); // 동쪽 끝
        world.getBlockAt(centerX - 2, glowstoneY, centerZ).setType(org.bukkit.Material.GLOWSTONE, false); // 서쪽 끝
        world.getBlockAt(centerX, glowstoneY, centerZ + 2).setType(org.bukkit.Material.GLOWSTONE, false); // 남쪽 끝
        world.getBlockAt(centerX, glowstoneY, centerZ - 2).setType(org.bukkit.Material.GLOWSTONE, false); // 북쪽 끝
    }

    private static void executePlayerKill() {
        Player player = RouletteManager.getParticipant();
        // 플레이어가 서버에 접속 중이고 존재하는지 확인
        if (player != null && player.isOnline()) {
            player.setHealth(0.0);
        }
    }
}