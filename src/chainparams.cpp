// Copyright (c) 2010 Satoshi Nakamoto
// Copyright (c) 2009-2015 The Bitcoin Core developers
// Distributed under the MIT software license, see the accompanying
// file COPYING or http://www.opensource.org/licenses/mit-license.php.

#include "chainparams.h"
#include "consensus/merkle.h"

#include "tinyformat.h"
#include "util.h"
#include "utilstrencodings.h"

#include <assert.h>

#include <boost/assign/list_of.hpp>

#include "chainparamsseeds.h"
#include "arith_uint256.h"

using namespace std;

static CBlock CreateGenesisBlock(const char* pszTimestamp, const CScript& genesisOutputScript, uint32_t nTime, uint32_t nNonce, uint32_t nBits, int32_t nVersion, const CAmount& genesisReward)
{
    CMutableTransaction txNew;
    txNew.nVersion = 1;
    txNew.nTime = nTime;
    txNew.vin.resize(1);
    txNew.vout.resize(1);
    txNew.vin[0].scriptSig = CScript() << 0 << CScriptNum(42) << vector<unsigned char>((const unsigned char*)pszTimestamp, (const unsigned char*)pszTimestamp + strlen(pszTimestamp));
    txNew.vout[0].nValue = genesisReward;

    CBlock genesis;
    genesis.nTime    = nTime;
    genesis.nBits    = nBits;
    genesis.nNonce   = nNonce;
    genesis.nVersion = nVersion;
    genesis.vtx.push_back(txNew);
    genesis.hashPrevBlock.SetNull();
    genesis.hashMerkleRoot = BlockMerkleRoot(genesis);
    return genesis;
}

/**
 * Build the genesis block. Note that the output of its generation
 * transaction cannot be spent since it did not originally exist in the
 * database.
 */
static CBlock CreateGenesisBlock(uint32_t nTime, uint32_t nNonce, uint32_t nBits, int32_t nVersion, const CAmount& genesisReward)
{
    const char* pszTimestamp = "A New Beginning for a Lasting Project - Quimera Labs 2026";
    const CScript genesisOutputScript = CScript() << ParseHex("04f4909745a0695b97b48872fe0b2e295c4ded58b28d819f54a19e1bae523ba9c5507dd178e971008a6da2ae2e2af3c8e0bdb37774f5127622f035fe682f72d334") << OP_CHECKSIG;
    return CreateGenesisBlock(pszTimestamp, genesisOutputScript, nTime, nNonce, nBits, nVersion, genesisReward);
}

/**
 * Main network
 */
/**
 * What makes a good checkpoint block?
 * + Is surrounded by blocks with reasonable timestamps
 *   (no blocks before with a timestamp after, none after with
 *    timestamp before)
 * + Contains no strange transactions
 */

class CMainParams : public CChainParams {
public:
    CMainParams() {
        strNetworkID = "main";
        consensus.nMaxReorganizationDepth = 500;
        consensus.nMajorityEnforceBlockUpgrade = 750;
        consensus.nMajorityRejectBlockOutdated = 950;
        consensus.nMajorityWindow = 1000;
        consensus.powLimit = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.posLimit = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.posLimitV2 = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.nTargetTimespan =  60 * 60;
        consensus.nTargetSpacingV1 = 2 * 60;
        consensus.nTargetSpacing = 2 * 60;
        consensus.BIP34Height = -1;
        consensus.BIP34Hash = uint256();
        consensus.fPowAllowMinDifficultyBlocks = false;
        consensus.fPowNoRetargeting = false;
        consensus.fPoSNoRetargeting = false;
        consensus.nRuleChangeActivationThreshold = 19; // 95% of 2
        consensus.nMinerConfirmationWindow = 20; // nTargetTimespan / nTargetSpacing
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].bit = 28;
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nStartTime = 1704067201; // January 1, 2024
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nTimeout = 1735603201; // December 31, 2024

        consensus.nProtocolV1RetargetingFixedTime = 1769817600;
        consensus.nProtocolV2Time = 1769817601;
        consensus.nProtocolV3Time = 1769817602;
        consensus.nLastPOWBlock = 110000;
        consensus.nStakeTimestampMask = 0xf; // 15
        consensus.nCoinbaseMaturity = 24;
        consensus.nStakeMinAge = 24 * 60 * 60; // 24 hours

        // The best chain should have at least this much work.
        consensus.nMinimumChainWork = uint256S("0x0000000000000000000000000000000000000000000000000000000000100001");

        /**
         * The message start string is designed to be unlikely to occur in normal data.
         * The characters are rarely used upper ASCII, not valid as UTF-8, and produce
         * a large 32-bit integer with any alignment.
         */
        pchMessageStart[0] = 0x32; // '2'
        pchMessageStart[1] = 0x78; // 'x'
        pchMessageStart[2] = 0x32; // '2'
        pchMessageStart[3] = 0x43; // 'C'
        nDefaultPort = 15190;
        nPruneAfterHeight = 100000;

        genesis = CreateGenesisBlock(1769817600, 184621, 0x1e0fffff, 1, 0);
        consensus.hashGenesisBlock = genesis.GetHash();
        assert(consensus.hashGenesisBlock == uint256S("0x00000eea834a06692bc4f56d6f0061631c72fd75431ce9e5d7f3b9d712dc3a9b"));
        assert(genesis.hashMerkleRoot == uint256S("0x8ec923e8d644631a549ddbd51cd350f597e29ca665979f01a501456bbc77f16b"));

        vSeeds.push_back(CDNSSeedData("161.97.176.125", "161.97.176.125"));
        vSeeds.push_back(CDNSSeedData("77.237.232.84", "77.237.232.84"));
        vSeeds.push_back(CDNSSeedData("161.97.176.125", "161.97.176.125"));
        vSeeds.push_back(CDNSSeedData("77.237.232.84", "77.237.232.84"));
        vSeeds.push_back(CDNSSeedData("75.119.137.26", "75.119.137.26"));
        vSeeds.push_back(CDNSSeedData("149.102.139.53", "149.102.139.53"));
        vSeeds.push_back(CDNSSeedData("144.91.107.244", "144.91.107.244"));
		vSeeds.push_back(CDNSSeedData("38.242.236.173", "38.242.236.173"));
        vSeeds.push_back(CDNSSeedData("seed.quimeralabs.org", "seed.quimeralabs.org"));
        vSeeds.push_back(CDNSSeedData("seed1.quimeralabs.org", "seed1.quimeralabs.org"));
        vSeeds.push_back(CDNSSeedData("seed2.quimeralabs.org", "seed2.quimeralabs.org"));
        vSeeds.push_back(CDNSSeedData("seed3.quimeralabs.org", "seed3.quimeralabs.org"));
        vSeeds.push_back(CDNSSeedData("seed4.quimeralabs.org", "seed4.quimeralabs.org"));
        vSeeds.push_back(CDNSSeedData("seed5.quimeralabs.org", "seed5.quimeralabs.org"));
        

        base58Prefixes[PUBKEY_ADDRESS] = std::vector<unsigned char>(1,3);
        base58Prefixes[SCRIPT_ADDRESS] = std::vector<unsigned char>(1,90);
        base58Prefixes[SECRET_KEY] = std::vector<unsigned char>(1,128);
        base58Prefixes[EXT_PUBLIC_KEY] = boost::assign::list_of(0x04)(0x88)(0xB2)(0x1E).convert_to_container<std::vector<unsigned char> >();
        base58Prefixes[EXT_SECRET_KEY] = boost::assign::list_of(0x04)(0x88)(0xAD)(0xE4).convert_to_container<std::vector<unsigned char> >();
        cashaddrPrefix = "2x2coin";

        vFixedSeeds = std::vector<SeedSpec6>(pnSeed6_main, pnSeed6_main + ARRAYLEN(pnSeed6_main));

        fMiningRequiresPeers = true;
        fDefaultConsistencyChecks = false;
        fRequireStandard = true;
        fMineBlocksOnDemand = false;
        fTestnetToBeDeprecatedFieldRPC = false;

        checkpointData = (CCheckpointData) {
                    boost::assign::map_list_of
                    (0, uint256S("0x00000eea834a06692bc4f56d6f0061631c72fd75431ce9e5d7f3b9d712dc3a9b")),
                    1769817600, // * UNIX timestamp of last checkpoint block
                    0,    // * total number of transactions between genesis and last checkpoint
                                //   (the tx=... number in the SetBestChain debug.log lines)
                    0      // * estimated number of transactions per day after checkpoint
        };
    }
};
static CMainParams mainParams;

/**
 * Testnet
 */
class CTestNetParams : public CChainParams {
public:
    CTestNetParams() {
        strNetworkID = "test";
        consensus.nMaxReorganizationDepth = 500;
        consensus.nMajorityEnforceBlockUpgrade = 750;
        consensus.nMajorityRejectBlockOutdated = 950;
        consensus.nMajorityWindow = 1000;
        consensus.powLimit = uint256S("0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.posLimit = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.posLimitV2 = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.nTargetTimespan =  6 * 60;
        consensus.nTargetSpacingV1 = 3 * 60;
        consensus.nTargetSpacing = 3 * 60;
        consensus.BIP34Height = -1;
        consensus.BIP34Hash = uint256();
        consensus.fPowAllowMinDifficultyBlocks = true;
        consensus.fPowNoRetargeting = false;
        consensus.fPoSNoRetargeting = false;
        consensus.nRuleChangeActivationThreshold = 1512; // 75% for testchains
        consensus.nMinerConfirmationWindow = 2; // nPowTargetTimespan / nPowTargetSpacing
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].bit = 28;
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nStartTime = 1704067201; // January 1, 2024
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nTimeout = 1735603201; // December 31, 2024

        consensus.nProtocolV1RetargetingFixedTime = 1769817600;
        consensus.nProtocolV2Time = 1769817601;
        consensus.nProtocolV3Time = 1769817602;
        consensus.nLastPOWBlock = 0x7fffffff;
        consensus.nStakeTimestampMask = 0xf;
        consensus.nCoinbaseMaturity = 10;
        consensus.nStakeMinAge = 8 * 60 * 60;

        pchMessageStart[0] = 0x4c;
        pchMessageStart[1] = 0x90;
        pchMessageStart[2] = 0x34;
        pchMessageStart[3] = 0xfb;
        nDefaultPort = 51884;

        // The best chain should have at least this much work.
        consensus.nMinimumChainWork = uint256S("0000000000000000000000000000000000000000000000000000000000100001");

        nPruneAfterHeight = 1000;

        genesis = CreateGenesisBlock(1769817600, 127143, 0x1f00ffff, 1, 0);
        consensus.hashGenesisBlock = genesis.GetHash();
        //assert(consensus.hashGenesisBlock == uint256S("0x00003522bd22dbda8b32c911eb058ff79d0d59d4156d6a6f66ddcfbbcb09a225"));
        //assert(genesis.hashMerkleRoot == uint256S("0x8a5c4b32d5b72be3f66c944e6f402d223aa437c90263dd9450ad15175dd74d9d"));

        vFixedSeeds.clear();
        vSeeds.clear();

        base58Prefixes[PUBKEY_ADDRESS] = std::vector<unsigned char>(1,51);
        base58Prefixes[SCRIPT_ADDRESS] = std::vector<unsigned char>(1,50);
        base58Prefixes[SECRET_KEY] =     std::vector<unsigned char>(1,239);
        base58Prefixes[EXT_PUBLIC_KEY] = boost::assign::list_of(0x04)(0x35)(0x87)(0xCF).convert_to_container<std::vector<unsigned char> >();
        base58Prefixes[EXT_SECRET_KEY] = boost::assign::list_of(0x04)(0x35)(0x83)(0x94).convert_to_container<std::vector<unsigned char> >();
        //cashaddrPrefix = "2x2test";

        vFixedSeeds = std::vector<SeedSpec6>(pnSeed6_test, pnSeed6_test + ARRAYLEN(pnSeed6_test));

        fMiningRequiresPeers = true;
        fDefaultConsistencyChecks = false;
        fRequireStandard = false;
        fMineBlocksOnDemand = false;
        fTestnetToBeDeprecatedFieldRPC = true;

        checkpointData = (CCheckpointData) {
            boost::assign::map_list_of
            ( 0, uint256S("0x00003522bd22dbda8b32c911eb058ff79d0d59d4156d6a6f66ddcfbbcb09a225")),
            1769817600,
            0,
            0
        };

    }
};
static CTestNetParams testNetParams;

/**
 * Regression test
 */
class CRegTestParams : public CChainParams {
public:
    CRegTestParams() {
        strNetworkID = "regtest";
        consensus.nMaxReorganizationDepth = 50;
        consensus.nMajorityEnforceBlockUpgrade = 51;
        consensus.nMajorityRejectBlockOutdated = 75;
        consensus.nMajorityWindow = 100;
        consensus.powLimit = uint256S("0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.posLimit = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.posLimitV2 = uint256S("00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        consensus.nTargetTimespan =  6 * 60;
        consensus.nTargetSpacingV1 = 64;
        consensus.nTargetSpacing = 3 * 60;
        consensus.BIP34Height = -1; // BIP34 has not necessarily activated on regtest
        consensus.BIP34Hash = uint256();
        consensus.fPowAllowMinDifficultyBlocks = true;
        consensus.fPowNoRetargeting = true;
        consensus.fPoSNoRetargeting = true;
        consensus.nRuleChangeActivationThreshold = 108;// 75% for regtest
        consensus.nMinerConfirmationWindow = 144; // Faster than normal for regtest (144 instead of 2016)
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].bit = 28;
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nStartTime = 1704067201; // January 1, 2024
        consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nTimeout = 1735603201; // December 31, 2024

        // The best chain should have at least this much work.
        consensus.nMinimumChainWork = uint256S("0x00");

        consensus.nProtocolV1RetargetingFixedTime = 1769817600;
        consensus.nProtocolV2Time = 1769817601;
        consensus.nProtocolV3Time = 1769817602;
        consensus.nLastPOWBlock = 1000;
        consensus.nStakeTimestampMask = 0xf;
        consensus.nCoinbaseMaturity = 10;
        consensus.nStakeMinAge = 1 * 60 * 60;

        pchMessageStart[0] = 0xc5;
        pchMessageStart[1] = 0xfc;
        pchMessageStart[2] = 0xcf;
        pchMessageStart[3] = 0x06;
        nDefaultPort = 35714;
        nPruneAfterHeight = 100000;

        genesis = CreateGenesisBlock(1769817600, 92159, 0x1f00ffff, 1, 0);
        consensus.hashGenesisBlock = genesis.GetHash();
        //assert(consensus.hashGenesisBlock == uint256S("0x00"));
        //assert(genesis.hashMerkleRoot == uint256S("0x00"));

        vFixedSeeds.clear(); //!< Regtest mode doesn't have any fixed seeds.
        vSeeds.clear();      //!< Regtest mode doesn't have any DNS seeds.

        base58Prefixes[PUBKEY_ADDRESS] = std::vector<unsigned char>(1,51);
        base58Prefixes[SCRIPT_ADDRESS] = std::vector<unsigned char>(1,50);
        base58Prefixes[SECRET_KEY] = std::vector<unsigned char>(1,178);
        base58Prefixes[EXT_PUBLIC_KEY] = boost::assign::list_of(0x04)(0x88)(0xB2)(0x1E).convert_to_container<std::vector<unsigned char> >();
        base58Prefixes[EXT_SECRET_KEY] = boost::assign::list_of(0x04)(0x88)(0xAD)(0xE4).convert_to_container<std::vector<unsigned char> >();
        //cashaddrPrefix = "2x2reg";

        fMiningRequiresPeers = false;
        fDefaultConsistencyChecks = true;
        fRequireStandard = false;
        fMineBlocksOnDemand = true;
        fTestnetToBeDeprecatedFieldRPC = false;

    }

    void UpdateBIP9Parameters(Consensus::DeploymentPos d, int64_t nStartTime, int64_t nTimeout)
    {
        consensus.vDeployments[d].nStartTime = nStartTime;
        consensus.vDeployments[d].nTimeout = nTimeout;
    }
};
static CRegTestParams regTestParams;

static CChainParams *pCurrentParams = 0;

const CChainParams &Params() {
    assert(pCurrentParams);
    return *pCurrentParams;
}

CChainParams& Params(const std::string& chain)
{
    if (chain == CBaseChainParams::MAIN)
            return mainParams;
    else if (chain == CBaseChainParams::TESTNET)
            return testNetParams;
    else if (chain == CBaseChainParams::REGTEST)
            return regTestParams;
    else
        throw std::runtime_error(strprintf("%s: Unknown chain %s.", __func__, chain));
}

void SelectParams(const std::string& network)
{
    SelectBaseParams(network);
    pCurrentParams = &Params(network);
}

void UpdateRegtestBIP9Parameters(Consensus::DeploymentPos d, int64_t nStartTime, int64_t nTimeout)
{
    regTestParams.UpdateBIP9Parameters(d, nStartTime, nTimeout);
}
