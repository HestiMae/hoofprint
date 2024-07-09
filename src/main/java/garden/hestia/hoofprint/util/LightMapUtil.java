package garden.hestia.hoofprint.util;

public class LightMapUtil {
    public static int[][] DAY = new int[][]{
        new int[]{0x0E0E0E, 0x151311, 0x1B1714, 0x221C17, 0x2A231B, 0x332A1F, 0x3E3325, 0x4A3E2C, 0x594B34, 0x6B5B40, 0x7F7051, 0x9A8A6A, 0xBBAF91, 0xE8E3D7, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x131313, 0x181715, 0x1F1B18, 0x26211B, 0x2E271F, 0x372E23, 0x423729, 0x4E4230, 0x5D4F38, 0x6F5F44, 0x837455, 0x9E8E6E, 0xBFB396, 0xECE7DB, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x171717, 0x1D1B1A, 0x23201C, 0x2B2520, 0x332B23, 0x3C3329, 0x473B2E, 0x534734, 0x62543D, 0x736448, 0x88795A, 0xA39373, 0xC4B89A, 0xF0ECE0, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x1C1C1C, 0x22201F, 0x282522, 0x2F2A25, 0x383028, 0x41382D, 0x4C4133, 0x584C39, 0x675942, 0x78694D, 0x8D7E5F, 0xA89878, 0xC9BD9F, 0xF6F1E5, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x222222, 0x282625, 0x2E2B27, 0x36302A, 0x3D362E, 0x463D32, 0x514639, 0x5E513F, 0x6C5F48, 0x7E6F53, 0x938364, 0xAD9E7E, 0xCEC2A5, 0xFBF7EB, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x292929, 0x2F2D2B, 0x35312E, 0x3C3731, 0x443D35, 0x4D443A, 0x584D3F, 0x645846, 0x73654E, 0x84755A, 0x998A6A, 0xB3A484, 0xD5C9AC, 0xFCFCF1, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x303030, 0x363433, 0x3C3935, 0x433E38, 0x4B443C, 0x544B40, 0x5F5447, 0x6C5F4D, 0x7A6D56, 0x8C7D61, 0xA19172, 0xBBAC8C, 0xDCD0B3, 0xFCFCF8, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x393939, 0x3F3D3B, 0x45413E, 0x4C4741, 0x544D45, 0x5D5449, 0x685D4F, 0x746856, 0x83755E, 0x94856A, 0xA99A7A, 0xC3B494, 0xE5D9BC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x434343, 0x494745, 0x4F4B48, 0x56514B, 0x5E574F, 0x675E53, 0x726759, 0x7E7260, 0x8D7F68, 0x9E8F74, 0xB3A484, 0xCDBE9D, 0xEFE3C6, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x4F4F4F, 0x555351, 0x5B5754, 0x625D57, 0x6A635B, 0x736A5F, 0x7E7365, 0x8A7E6C, 0x998B74, 0xAA9B80, 0xBFB090, 0xD9CAA9, 0xF9EFD2, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x5D5D5D, 0x63615F, 0x696562, 0x706B65, 0x787169, 0x81786D, 0x8C8173, 0x988C7A, 0xA79982, 0xB9A98E, 0xCDBE9E, 0xE7D8B7, 0xFCFCE0, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x6E6E6E, 0x747271, 0x7A7773, 0x817C77, 0x8A827A, 0x938A7F, 0x9E9285, 0xAA9D8B, 0xB9AB94, 0xCABA9F, 0xDFCFB0, 0xF8EAC9, 0xFCFCF0, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x848484, 0x8A8886, 0x908D89, 0x97928C, 0x9F9890, 0xA89F94, 0xB3A89B, 0xBFB3A1, 0xCEC0AA, 0xDFD0B5, 0xF5E5C6, 0xFCFCDF, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xA0A0A0, 0xA6A4A2, 0xACA9A5, 0xB3AEA8, 0xBBB4AC, 0xC4BBB0, 0xCFC4B7, 0xDBCFBD, 0xEADCC6, 0xF8ECD1, 0xFCFCE2, 0xFCFCFA, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xC5C5C5, 0xCBC9C8, 0xD1CECB, 0xD8D3CE, 0xE1D9D1, 0xEAE1D6, 0xF5E9DC, 0xFAF4E2, 0xFCFCEB, 0xFCFCF5, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xFAFAFA, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC}
    };
    public static int[][] NIGHT = new int[][]{
        new int[]{0x0E0E0E, 0x151311, 0x1B1714, 0x221C17, 0x2A231B, 0x332A1F, 0x3E3325, 0x4A3E2C, 0x594B34, 0x6B5B40, 0x7F7051, 0x9A8A6A, 0xBBAF91, 0xE8E3D7, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x0F0F0F, 0x151312, 0x1B1815, 0x221D18, 0x2A231C, 0x332A20, 0x3E3326, 0x4B3F2D, 0x594C35, 0x6B5C41, 0x7F7052, 0x9A8B6B, 0xBBAF92, 0xE8E4D8, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x0F0F11, 0x161413, 0x1C1816, 0x231D19, 0x2B241D, 0x352B22, 0x3F3427, 0x4B3F2E, 0x5A4C36, 0x6C5C42, 0x807153, 0x9B8B6C, 0xBCB094, 0xE9E4D9, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x101012, 0x161414, 0x1C1917, 0x231E1A, 0x2B241E, 0x352C22, 0x3F3428, 0x4C402F, 0x5A4D37, 0x6C5D43, 0x817153, 0x9C8C6D, 0xBCB095, 0xE9E5DA, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x111113, 0x171516, 0x1D1918, 0x241F1C, 0x2C251F, 0x362C24, 0x40362A, 0x4C4030, 0x5B4D39, 0x6D5D44, 0x817255, 0x9C8D6E, 0xBDB196, 0xEAE5DB, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x121215, 0x181617, 0x1E1A1A, 0x251F1D, 0x2D2621, 0x372D25, 0x41362B, 0x4E4132, 0x5C4E3A, 0x6E5E46, 0x827356, 0x9D8D6F, 0xBEB298, 0xEBE6DD, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x121217, 0x181619, 0x1E1B1C, 0x25201F, 0x2E2623, 0x382E27, 0x42372D, 0x4E4234, 0x5D4F3C, 0x6F5F48, 0x837458, 0x9E8F71, 0xBFB39A, 0xECE7DF, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x131319, 0x19171B, 0x1F1C1E, 0x272121, 0x2F2725, 0x392F29, 0x43382E, 0x4F4336, 0x5E503E, 0x6F604A, 0x84755A, 0x9F9073, 0xC0B49C, 0xEDE8E1, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x15151B, 0x1B191E, 0x211D20, 0x282223, 0x302927, 0x3A312B, 0x443A31, 0x504438, 0x5F5141, 0x71614C, 0x85765D, 0xA09176, 0xC1B59E, 0xEEE9E3, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x16161E, 0x1C1A20, 0x221E23, 0x2A2426, 0x312A2A, 0x3B322E, 0x453B33, 0x52463B, 0x605243, 0x72634F, 0x867760, 0xA19279, 0xC2B6A1, 0xEFEAE6, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x181821, 0x1E1C24, 0x242027, 0x2B252A, 0x332C2D, 0x3D3432, 0x473D37, 0x53473D, 0x625447, 0x746452, 0x887963, 0xA3947C, 0xC4B8A4, 0xF1ECE9, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x1A1A25, 0x201E28, 0x26222B, 0x2D272E, 0x352E31, 0x3F3636, 0x493F3B, 0x564942, 0x64564B, 0x766657, 0x8A7B67, 0xA59680, 0xC6BAA8, 0xF3EEEE, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x1C1C2B, 0x22202D, 0x282530, 0x302A33, 0x373137, 0x41383B, 0x4B4140, 0x584C47, 0x66594F, 0x78695C, 0x8D7D6C, 0xA89985, 0xC8BCAE, 0xF5F1F3, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x1F1F31, 0x252333, 0x2B2837, 0x332E3A, 0x3B343D, 0x443C42, 0x4F4447, 0x5C4F4D, 0x6A5C56, 0x7B6C62, 0x918173, 0xAB9C8C, 0xCCC1B4, 0xF8F4FA, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x242539, 0x2A293C, 0x302D3F, 0x373243, 0x3F3946, 0x49404B, 0x534950, 0x605356, 0x6E605F, 0x80706A, 0x95857B, 0xAFA095, 0xD1C5BC, 0xFBF8FC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x2A2B46, 0x302F48, 0x36334B, 0x3D384E, 0x453F52, 0x4F4656, 0x594F5C, 0x665963, 0x74666B, 0x867677, 0x9B8B88, 0xB5A6A1, 0xD7CBC9, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC}
    };
    public static int[][] NETHER = new int[][]{
        new int[]{0x473E35, 0x4C4338, 0x52473B, 0x594E3E, 0x605442, 0x685C47, 0x73654D, 0x7E6F54, 0x8B7C5E, 0x9B8C6C, 0xAE9F7F, 0xC7B999, 0xE5DCC4, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x4B4238, 0x50463B, 0x564B3F, 0x5D5142, 0x645846, 0x6C5F4B, 0x766851, 0x827358, 0x8F8062, 0x9E8F6F, 0xB2A383, 0xCABC9D, 0xE9DFC7, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x4F463C, 0x544B40, 0x5A4F43, 0x615646, 0x685C4A, 0x70644F, 0x7A6C55, 0x86775C, 0x938466, 0xA39373, 0xB6A787, 0xCEC1A1, 0xEDE3CC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x544B41, 0x584F44, 0x5E5447, 0x65594B, 0x6D604F, 0x756853, 0x7F7159, 0x8A7B61, 0x97886B, 0xA79878, 0xBAAB8B, 0xD3C5A6, 0xF1E8D0, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x595046, 0x5D5449, 0x64594D, 0x6B5F50, 0x726554, 0x7A6D59, 0x84765F, 0x908166, 0x9C8D70, 0xAC9D7D, 0xC0B090, 0xD8CAAB, 0xF7EDD5, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x5F564C, 0x635A50, 0x695F52, 0x706456, 0x786B5A, 0x80735F, 0x8A7C64, 0x96876C, 0xA29376, 0xB2A383, 0xC6B696, 0xDED0B1, 0xFAF3DB, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x655D53, 0x6A6156, 0x706659, 0x776B5C, 0x7E7260, 0x877A65, 0x91836B, 0x9C8D73, 0xA99A7C, 0xB9A98A, 0xCCBD9D, 0xE5D7B7, 0xFBFAE2, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x6D645B, 0x72695E, 0x786D61, 0x7F7364, 0x867968, 0x8E816D, 0x998A73, 0xA4957A, 0xB1A284, 0xC1B191, 0xD4C5A4, 0xECDFBF, 0xFCFCEA, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x766D64, 0x7B7267, 0x81766A, 0x887C6D, 0x8F8371, 0x978A76, 0xA2947C, 0xAD9E83, 0xBAAB8D, 0xCABA9A, 0xDDCEAD, 0xF6E8C8, 0xFCFCF2, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x81786E, 0x857C71, 0x8C8175, 0x938778, 0x998D7C, 0xA29581, 0xAC9E87, 0xB8A98E, 0xC4B598, 0xD4C4A5, 0xE8D8B8, 0xFAF2D3, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x8E857B, 0x92897E, 0x988E81, 0x9F9385, 0xA69A89, 0xAFA28E, 0xB9AA93, 0xC5B69B, 0xD1C2A5, 0xE1D1B2, 0xF5E5C4, 0xFCFCE0, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x9D958B, 0xA2998E, 0xA89E91, 0xAFA394, 0xB6A998, 0xBFB29D, 0xC9BAA3, 0xD4C5AB, 0xE1D2B4, 0xF1E1C1, 0xFAF5D4, 0xFCFCEF, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xB1A89E, 0xB6ACA1, 0xBCB1A5, 0xC3B7A8, 0xC9BDAC, 0xD2C5B1, 0xDCCEB7, 0xE7D8BE, 0xF5E6C8, 0xFAF4D5, 0xFCFCE8, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xCAC1B8, 0xCFC6BB, 0xD5CABE, 0xDCD0C1, 0xE3D6C5, 0xEBDECA, 0xF6E7D0, 0xF9F1D7, 0xFCFCE1, 0xFCFCEE, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xECE3D9, 0xF0E7DC, 0xF7ECDF, 0xF9F1E3, 0xFBF8E7, 0xFCFCEC, 0xFCFCF0, 0xFCFCF8, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC},
        new int[]{0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC}
    };
    public static int[][] END = new int[][]{
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC},
        new int[]{0x414B44, 0x454E46, 0x495148, 0x4F564B, 0x565B4E, 0x5D6152, 0x656856, 0x6E705B, 0x7A7A62, 0x87876B, 0x989778, 0xACAC8B, 0xC7C9AB, 0xE9F1E2, 0xFCFCFC, 0xFCFCFC}
    };
}
