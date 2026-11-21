# Rage Mod v2 (Fabric 1.20.1)

## Etaplar
- **Rage bar** hasar aldıkça dolar → **R** = RAGE 1.
- Rage açıkken bar bir sonraki etap için tekrar dolar (daha zor). Dolunca bar **gökkuşağı** olur, wither şarj sesi gelir → **R** = sonraki etap (wither doğma sesi).
- **Etap 1:** can x1.5, biraz zırh. Hafif güçlü.
- **Etap 2:** can x3, daha fazla zırh ve hasar. Yumrukla duvar kırma + hava dalgası açılır.
- **Etap 3:** can x5, neredeyse yenilmez, dash hasar verir, bütün yaratıklar kaçar,
  yürüdüğün yer ruh kumuna döner (seni yavaşlatmaz), ara ara wither çığlıkları, müzik.
- Evcil hayvanın ölürse bar anında dolar, metre 6 dk azalmaz.

## Kontroller
| Tuş | Ne yapar |
|---|---|
| **R** | Rage başlat / bar doluysa etap atla / değilse bağır (20 sn) |
| **P** | Rage'i kapat (sonrası zehir + yavaşlık + baş dönmesi) |
| **F** | Tekme |
| **G** | Dash (etap 3'te hasar verir) |
| **Sağ tık** | Yaratık tut / fırlat, alet fırlat, basılı tut = blok fırlat |
| **Shift + Space** | Süper zıplama (havada 1 ekstra zıplama) |
| **Havada CTRL** | Yere çakıl: patlama + şimşek |
| **Shift + M1 bloğa** (etap 2+) | Yumrukla büyük alanı parçala |
| **Havada M1 / yerde Shift + M1 boşluğa** (etap 2+) | Hava dalgası |

Yaratıkların canı azaldıkça kol/bacakları kopar.

## Etap 3 müziği
Telifli şarkıları moda koyamadığım için müziği sen eklersin:
1. `RageMuzik` klasörünü `%appdata%\.minecraft\resourcepacks` içine koy.
2. Şarkını `.ogg` yap, adını `rage_stage3.ogg` koy, `RageMuzik/assets/ragemod/sounds/` içine at.
3. Oyunda Seçenekler → Kaynak Paketleri → RageMuzik'i aktif et.
Müzik "Müzik Kutuları/Nota Blokları" ses ayarından çalar.
