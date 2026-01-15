# (업데이트) Minecraft RPG 플러그인 통합 설계 (1.21.x 전범위 지원)

목표: **바닐라 전투/아이템 성능을 사실상 무시**하고, 플러그인 정의 기반으로
- 스텟 / 무기 스텟(공속 포함) / 직업(전투·생활) / 강화
- 토지(청크) 구매(사유지/길드지)
- 건축가(유상 크리에이티브) + 건축 의뢰(에스크로 계약)
를 구현한다.

최종 목표: **개인 집 / 길드 하우스**를 자연스럽게 만들게 하고,
“직접 건축은 오래 걸리니 **건축가에게 의뢰**하도록 유도”한다.

---

## 1) 버전/호환성 원칙 (1.21.xx 전체 지원)
- 권장: Paper 1.21.x
- NMS 금지(패치 범위 안정성)
- 아이템/상태 데이터는 PDC(NamespacedKey)로 저장(로어는 표시용)
- 텍스트 UI: Adventure/MiniMessage 권장

---

## 2) “기본 시스템 무시” 정책
- 바닐라 무기 공격력/갑옷 방어력은 **최종 데미지 재계산으로 덮어쓰기**
- 인챈트/포션은 기본 “무시 또는 옵션화(추가 보정만 허용)”
- 넉백/연출은 유지 가능(데미지 로직만 커스텀)

---

## 3) 모듈 구성
- core, data, stats, items, combat, enhance, jobs, gui, commands
- builder(유상 크리에이티브)
- land(청크 구매/보호)
- contracts(건축 의뢰/에스크로)
- hooks(선택): Vault/PlaceholderAPI/ItemsAdder/Oraxen/외부 길드 연동

---

## 4) 스텟(단순/직관 6개)
- ATK / DEF / HP / CRIT(%) / CRIT_DMG(배율) / PEN(% 또는 고정)

---

## 5) 무기 스텟(공격 속도 포함)

### WeaponData(무기)
- weaponType (SWORD/AXE/RANGED/STAFF 등)
- baseWpnAtk
- bonusStats (ATK/CRIT/PEN 등)
- enhanceLevel (+0~+10)
- attackSpeed ✅ (추가)
  - 권장 단위: APS(Attacks Per Second)
  - cooldownTicks = round(20 / APS)
- (원거리 전용) projectileProfile(즉발 사격용)
  - projectileType(ARROW/SNOWBALL 등)
  - projectileSpeed
  - spread(탄퍼짐)
  - range(옵션)
  - hitbox/헤드샷(옵션)

저장:
- 무기 데이터는 PDC에 저장(위조 방지)
- 로어는 표시용 렌더링

---

## 6) 전투 시스템(커스텀 데미지 + 공속 적용)

### 공속(attackSpeed) 적용
- 플레이어 lastAttackTick 기록
- 이벤트 진입 시 cooldownTicks 미만이면
  - 데미지 0 또는 이벤트 취소(정책 옵션)
- 통과 시 데미지 계산 후 lastAttackTick 갱신

### 데미지 공식(예시)
- A = attacker.ATK + weapon.baseWpnAtk + weapon.bonusATK
- effectiveDEF = defender.DEF * (1 - PEN%) - PEN_FLAT
- base = max(minDamage, A - max(0, effectiveDEF))
- 크리 성공 시 base *= CRIT_DMG
- 직업 보정 base *= jobMultiplier
- event.setDamage(final)

---

## 7) “활 당김 제거”: 즉발 원거리(화살 없이 투사체 스폰)

### 핵심 아이디어
- 바닐라 활의 “당김(차징)”을 쓰지 않고,
- 우클릭 순간 **서버가 투사체(Projectile)를 직접 생성**하고,
- 그 투사체로 데미지를 계산한다.
- 화살 아이템은 필요 없다.

### 구현 개요
- PlayerInteractEvent(우클릭):
  1) 손 아이템이 “RPG 원거리 무기”인지 식별
  2) 공속(attackSpeed) 쿨다운 체크
  3) 이벤트 취소(바닐라 사용 막기)
  4) Projectile spawn(예: Arrow)
  5) 투사체에 PDC로 “RPG 투사체” 마킹 + weaponId 저장
  6) pickup 비활성화, 중력/관통 등 옵션 적용

- EntityDamageByEntityEvent:
  - damager가 Projectile이고 “RPG 투사체” 태그가 있으면
  - shooter 스텟 + 무기 스텟 + 직업 보정으로 최종 데미지 산출
  - event.setDamage(final)

### 주의(UX)
- “완전한 당김 제거 체감”을 원하면,
  - **베이스 아이템을 BOW가 아닌 우클릭 아이템**(예: stick류/도구류)로 두고,
  - 리소스팩(또는 ItemsAdder/Oraxen)으로 활/총처럼 보이게 하는 게 가장 깔끔.
- 그래도 BOW 기반 커스텀 아이템에도 “우클릭 → 취소 → 즉발 투사체”는 적용 가능하나,
  클라이언트 애니메이션/체감이 약간 남을 수 있다(환경 따라 다름).

---

## 8) 커스텀 아이템(리소스팩)에도 적용 가능?

가능 조건: “어떤 아이템이 RPG 원거리 무기인지”를 식별할 수 있어야 한다.

식별 방식(우선순위 권장):
1) PDC(자체 태그)
2) CustomModelData(리소스팩 단독)
3) ItemsAdder/Oraxen 등 외부 플러그인 ID 훅(선택)

→ 식별된 아이템을 weapons.yml에서 weaponId로 매핑하면 즉발 사격/데미지 시스템에 동일 적용.

---

## 9) 강화 시스템
- +0~+10, 확률/실패패널티/보호권
- 강화는 무기 스텟(baseWpnAtk 등)에 반영
- 강화 GUI 제공

---

## 10) 직업: 전투 직업 + 생활 직업

### 전투 직업(combatJob)
- 패시브 중심(초기 2~3개)
- 전투 보정(데미지/크리/관통/방어 등)

### 생활 직업(lifeJob)
- 대장장이: 강화재료/무기 제작/개조
- 요리사: 버프 음식(스텟 버프)
- 건축가: 유상 크리에이티브(돈으로 즉시 건축)

---

## 11) 건축가(유상 크리에이티브)

### 핵심
- 빌드 모드 ON/OFF
- 블록 “설치 순간” 돈 차감(화살과 동일한 철학: 사용 순간 결제)
- 레벨이 오를수록 할인/해금 블록 증가
- 악용 방지:
  - 팔레트 아이템 이동/드롭/상자 보관/제작 투입 금지
  - 계약 영역/허용 영역 밖 설치 차단
  - 로그아웃/크래시 복구

---

## 12) 토지(청크) 구매: 사유지/길드지
- 청크 단위 구매/확장
- 보호(설치/파괴/상호작용/엔티티 등)
- 신뢰(허용) 관리, 경계 표시
- 개인집/길드하우스 기반

---

## 13) 건축 의뢰(계약): 에스크로 + 임시 권한

### 에스크로(강력 권장)
- 의뢰 생성 시:
  - 재료 예산(materialBudget) + 인건비(laborFee) 예치
- 건축 중 블록 설치 비용은 에스크로에서 차감
- 완료 시:
  - 남은 예산 환불/이월
  - 인건비 지급(승인/자동승인 정책)

### 임시 권한
- 계약 ACTIVE 동안 건축가에게 해당 청크(영역) 빌드 권한 부여
- 종료/취소 시 즉시 회수
- 건축가 빌드 모드는 “계약 영역 내에서만” 사용 가능(기본 정책)

---

## 14) GUI/명령어(요약)
- /rpg: 메인
- /stats /job /enhance /land /builder /contract
- 토지/의뢰/건축 팔레트/무기 도감(옵션) GUI

---

## 15) 저장소(DB 지원)
- SQLite 기본, MySQL/MariaDB 선택
- JDBC + HikariCP
- 캐시 + 주기 저장 + 종료/퇴장 저장
- schema_version 마이그레이션

---

## 16) 외부 플러그인 의존성(설치 필요)
사실상 필수(돈 기반):
- Vault + 경제 플러그인(예: EssentialsX Economy)

선택:
- PlaceholderAPI(표시 공유)
- ItemsAdder/Oraxen(커스텀 아이템/모델)
- CoreProtect(로그/롤백)
- WorldGuard/PlotSquared(구역/플롯 연동; 필수는 아님)

---


무기에 성장 시스템을 추가할꺼야 등급을 추가해서 등급에 따라 무기의 추가 스텟이 해방되는 형식이고 해방되는 스텟의 종류와 값은 랜덤이야 예를 들어
  일반 등급은 추가 스텟이 없고 레어가 되면 추가 스텟 하나가 해방되서 공격력 +3 추가 같이 늘어나고 값의 범위도 등급이 올라갈 수록 커지는 시스템이야
  그리고 등급 강화는 대장장이만 가능하게 할거고 추가 스텟의 종류와 값을 바꾸는 메이플 스토리의 큐브와 비슷한 시스템도 구현해